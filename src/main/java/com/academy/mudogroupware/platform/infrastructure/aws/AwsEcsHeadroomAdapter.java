package com.academy.mudogroupware.platform.infrastructure.aws;

import com.academy.mudogroupware.platform.application.port.EcsHeadroomPort;
import com.academy.mudogroupware.platform.domain.exception.PlatformErrorCode;
import com.academy.mudogroupware.platform.domain.exception.PlatformException;
import com.academy.mudogroupware.platform.domain.model.AcademyRuntime;
import com.academy.mudogroupware.platform.domain.model.OperationalMetrics.EcsHostHeadroom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;
import software.amazon.awssdk.services.ecs.model.Resource;

@Component
@ConditionalOnProperty(prefix = "platform.dashboard", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AwsEcsHeadroomAdapter implements EcsHeadroomPort {
  private final EcsClient ecsClient;

  @Override
  public List<EcsHostHeadroom> findHeadrooms(List<AcademyRuntime> academies) {
    try {
      Map<String, List<AcademyRuntime>> byCluster = academies.stream()
          .collect(Collectors.groupingBy(AcademyRuntime::ecsCluster));
      List<EcsHostHeadroom> result = new ArrayList<>();
      byCluster.forEach((cluster, clusterAcademies) -> result.addAll(findClusterHeadrooms(cluster, clusterAcademies)));
      return result;
    } catch (Exception exception) {
      throw new PlatformException(PlatformErrorCode.METRICS_UNAVAILABLE, exception);
    }
  }

  private List<EcsHostHeadroom> findClusterHeadrooms(String cluster, List<AcademyRuntime> academies) {
    List<String> instances = ecsClient.listContainerInstances(request -> request.cluster(cluster))
        .containerInstanceArns();
    if (instances.isEmpty()) return List.of();

    Map<String, List<String>> academiesByInstance = academies.stream()
        .collect(Collectors.toMap(
            AcademyRuntime::code,
            academy -> ecsClient.listTasks(request -> request.cluster(cluster).serviceName(academy.ecsService()))
                .taskArns(),
            (first, second) -> first));
    Map<String, List<String>> academyCodesByInstance = academiesByInstance.entrySet().stream()
        .filter(entry -> !entry.getValue().isEmpty())
        .flatMap(entry -> ecsClient.describeTasks(request -> request.cluster(cluster).tasks(entry.getValue()))
            .tasks().stream().filter(task -> task.containerInstanceArn() != null)
            .map(task -> Map.entry(task.containerInstanceArn(), entry.getKey())))
        .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

    return ecsClient.describeContainerInstances(request -> request.cluster(cluster).containerInstances(instances))
        .containerInstances().stream().map(instance -> headroom(cluster, instance, academyCodesByInstance)).toList();
  }

  private EcsHostHeadroom headroom(
      String cluster, ContainerInstance instance, Map<String, List<String>> academyCodesByInstance) {
    int registeredCpu = integerResource(instance.registeredResources(), "CPU");
    int registeredMemory = integerResource(instance.registeredResources(), "MEMORY");
    int remainingCpu = integerResource(instance.remainingResources(), "CPU");
    int remainingMemory = integerResource(instance.remainingResources(), "MEMORY");
    String hostId = instance.ec2InstanceId();
    return new EcsHostHeadroom(
        cluster,
        hostId,
        registeredCpu,
        registeredMemory,
        remainingCpu,
        remainingMemory,
        academyCodesByInstance.getOrDefault(instance.containerInstanceArn(), List.of()));
  }

  private int integerResource(List<Resource> resources, String name) {
    return resources.stream()
        .filter(resource -> name.equals(resource.name()))
        .findFirst()
        .map(resource -> resource.integerValue() == null ? 0 : resource.integerValue())
        .orElse(0);
  }
}

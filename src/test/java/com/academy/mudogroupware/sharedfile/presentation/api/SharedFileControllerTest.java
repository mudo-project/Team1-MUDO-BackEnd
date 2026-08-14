package com.academy.mudogroupware.sharedfile.presentation.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.academy.mudogroupware.global.infrastructure.security.jwt.JwtTokenProvider;
import com.academy.mudogroupware.global.presentation.security.AuthUser;
import com.academy.mudogroupware.global.presentation.security.JwtAuthenticationConverter;
import com.academy.mudogroupware.sharedfile.application.port.DriveBinary;
import com.academy.mudogroupware.sharedfile.application.port.GoogleWorkspaceFileType;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileItemsView;
import com.academy.mudogroupware.sharedfile.application.query.SharedFileRootView;
import com.academy.mudogroupware.sharedfile.application.usecase.CreateGoogleWorkspaceFileUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.CreateSharedFolderUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.DownloadSharedFileUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.GetSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.GetSharedFileRootUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.ListSharedFileItemsUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.MoveSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.RecreateSharedFileRootUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.RenameSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.SearchSharedFileItemsUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.TrashSharedFileItemUseCase;
import com.academy.mudogroupware.sharedfile.application.usecase.UploadSharedFileUseCase;

@WebMvcTest(SharedFileController.class)
@Import(SharedFileControllerTest.MethodSecurityConfiguration.class)
class SharedFileControllerTest {

    private static final AuthUser AUTH_USER = new AuthUser(10L, "user", 3L, "MEMBER");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSharedFileRootUseCase getSharedFileRootUseCase;
    @MockitoBean
    private RecreateSharedFileRootUseCase recreateSharedFileRootUseCase;
    @MockitoBean
    private ListSharedFileItemsUseCase listSharedFileItemsUseCase;
    @MockitoBean
    private GetSharedFileItemUseCase getSharedFileItemUseCase;
    @MockitoBean
    private SearchSharedFileItemsUseCase searchSharedFileItemsUseCase;
    @MockitoBean
    private CreateSharedFolderUseCase createSharedFolderUseCase;
    @MockitoBean
    private UploadSharedFileUseCase uploadSharedFileUseCase;
    @MockitoBean
    private CreateGoogleWorkspaceFileUseCase createGoogleWorkspaceFileUseCase;
    @MockitoBean
    private RenameSharedFileItemUseCase renameSharedFileItemUseCase;
    @MockitoBean
    private MoveSharedFileItemUseCase moveSharedFileItemUseCase;
    @MockitoBean
    private TrashSharedFileItemUseCase trashSharedFileItemUseCase;
    @MockitoBean
    private DownloadSharedFileUseCase downloadSharedFileUseCase;
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;
    @MockitoBean
    private JwtAuthenticationConverter jwtAuthenticationConverter;

    @Test
    void getRootReturnsReadyState() throws Exception {
        when(getSharedFileRootUseCase.getRoot()).thenReturn(new SharedFileRootView(true, "root-id"));

        mockMvc.perform(get("/api/shared-files/root").with(authentication(manageUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SHAREDFILE_200_1"))
                .andExpect(jsonPath("$.data.ready").value(true))
                .andExpect(jsonPath("$.data.rootId").value("root-id"));
    }

    @Test
    void getRootRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/shared-files/root"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRootReturns403WithoutManageAuthority() throws Exception {
        mockMvc.perform(get("/api/shared-files/root").with(authentication(noAuthorityUser())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getSharedFileRootUseCase);
    }

    @Test
    void recreateRootRequiresRootManageAuthority() throws Exception {
        mockMvc.perform(post("/api/shared-files/root/recreation")
                        .with(authentication(manageUser()))
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verifyNoInteractions(recreateSharedFileRootUseCase);
    }

    @Test
    void recreateRootSucceedsWithRootManageAuthority() throws Exception {
        when(recreateSharedFileRootUseCase.recreate()).thenReturn(new SharedFileRootView(true, "root-id"));

        mockMvc.perform(post("/api/shared-files/root/recreation")
                        .with(authentication(rootManageUser()))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SHAREDFILE_200_2"))
                .andExpect(jsonPath("$.data.rootId").value("root-id"));
    }

    @Test
    void rootManageAuthorityAloneCannotAccessContentApis() throws Exception {
        mockMvc.perform(get("/api/shared-files/root").with(authentication(rootManageUser())))
                .andExpect(status().isForbidden());

        verifyNoInteractions(getSharedFileRootUseCase);
    }

    @Test
    void getItemsReturnsCursorFields() throws Exception {
        SharedFileItemsView view = new SharedFileItemsView(
                List.of(item("item-1", "수업계획.docx")), true, "next-cursor");
        when(listSharedFileItemsUseCase.list(eq("root-id"), isNull(), eq(20))).thenReturn(view);

        mockMvc.perform(get("/api/shared-files/items")
                        .param("parentId", "root-id")
                        .with(authentication(manageUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value("item-1"))
                .andExpect(jsonPath("$.data.hasNext").value(true))
                .andExpect(jsonPath("$.data.nextCursor").value("next-cursor"));
    }

    @Test
    void getItemReturnsDetail() throws Exception {
        when(getSharedFileItemUseCase.get("item-1")).thenReturn(item("item-1", "수업계획.docx"));

        mockMvc.perform(get("/api/shared-files/items/item-1").with(authentication(manageUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("수업계획.docx"));
    }

    @Test
    void searchItemsForwardsKeywordAndFilters() throws Exception {
        SharedFileItemsView view = new SharedFileItemsView(List.of(), false, null);
        when(searchSharedFileItemsUseCase.search("계획", null, null, 20)).thenReturn(view);

        mockMvc.perform(get("/api/shared-files/items/search")
                        .param("keyword", "계획")
                        .with(authentication(manageUser())))
                .andExpect(status().isOk());

        verify(searchSharedFileItemsUseCase).search("계획", null, null, 20);
    }

    @Test
    void createFolderReturns201() throws Exception {
        when(createSharedFolderUseCase.create("root-id", "수업 운영"))
                .thenReturn(item("folder-1", "수업 운영"));

        mockMvc.perform(post("/api/shared-files/folders")
                        .with(authentication(manageUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"root-id\",\"name\":\"수업 운영\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("folder-1"));
    }

    @Test
    void createFolderSucceedsWithoutParentId() throws Exception {
        when(createSharedFolderUseCase.create(null, "수업 운영"))
                .thenReturn(item("folder-1", "수업 운영"));

        mockMvc.perform(post("/api/shared-files/folders")
                        .with(authentication(manageUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"수업 운영\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("folder-1"));
    }

    @Test
    void createFolderRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/shared-files/folders")
                        .with(authentication(manageUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"root-id\",\"name\":\"\"}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(createSharedFolderUseCase);
    }

    @Test
    void uploadItemReturns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "수업계획.docx", "application/octet-stream", "content".getBytes());
        when(uploadSharedFileUseCase.upload(eq("root-id"), eq("수업계획.docx"), any(), any()))
                .thenReturn(item("item-1", "수업계획.docx"));

        mockMvc.perform(multipart("/api/shared-files/items/upload")
                        .file(file)
                        .param("parentId", "root-id")
                        .with(authentication(manageUser()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("item-1"));
    }

    @Test
    void uploadItemSucceedsWithoutParentId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "수업계획.docx", "application/octet-stream", "content".getBytes());
        when(uploadSharedFileUseCase.upload(eq(null), eq("수업계획.docx"), any(), any()))
                .thenReturn(item("item-1", "수업계획.docx"));

        mockMvc.perform(multipart("/api/shared-files/items/upload")
                        .file(file)
                        .with(authentication(manageUser()))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("item-1"));
    }

    @Test
    void createGoogleFileReturns201() throws Exception {
        when(createGoogleWorkspaceFileUseCase.create("root-id", "9월 수업계획", GoogleWorkspaceFileType.DOCS))
                .thenReturn(item("gdoc-1", "9월 수업계획"));

        mockMvc.perform(post("/api/shared-files/google-files")
                        .with(authentication(manageUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"parentId\":\"root-id\",\"name\":\"9월 수업계획\",\"type\":\"DOCS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("gdoc-1"));
    }

    @Test
    void createGoogleFileSucceedsWithoutParentId() throws Exception {
        when(createGoogleWorkspaceFileUseCase.create(null, "9월 수업계획", GoogleWorkspaceFileType.DOCS))
                .thenReturn(item("gdoc-1", "9월 수업계획"));

        mockMvc.perform(post("/api/shared-files/google-files")
                        .with(authentication(manageUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"9월 수업계획\",\"type\":\"DOCS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value("gdoc-1"));
    }

    @Test
    void updateItemRenamesWhenOnlyNameGiven() throws Exception {
        when(renameSharedFileItemUseCase.rename("item-1", "새이름.docx"))
                .thenReturn(item("item-1", "새이름.docx"));

        mockMvc.perform(patch("/api/shared-files/items/item-1")
                        .with(authentication(manageUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"새이름.docx\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("새이름.docx"));

        verifyNoInteractions(moveSharedFileItemUseCase);
    }

    @Test
    void updateItemRejectsRequestWithoutNameOrParentId() throws Exception {
        mockMvc.perform(patch("/api/shared-files/items/item-1")
                        .with(authentication(manageUser()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(renameSharedFileItemUseCase, moveSharedFileItemUseCase);
    }

    @Test
    void trashItemReturns204() throws Exception {
        mockMvc.perform(delete("/api/shared-files/items/item-1")
                        .with(authentication(manageUser()))
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(trashSharedFileItemUseCase).trash("item-1");
    }

    @Test
    void downloadItemReturnsContentDisposition() throws Exception {
        when(downloadSharedFileUseCase.download(eq("item-1"), isNull()))
                .thenReturn(new DriveBinary("content".getBytes(), "수업계획.docx", "application/octet-stream"));

        mockMvc.perform(get("/api/shared-files/items/item-1/download").with(authentication(manageUser())))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")));
    }

    private SharedFileItemView item(String id, String name) {
        return new SharedFileItemView(id, name, "application/octet-stream", "https://drive.google.com/view",
                true, LocalDateTime.of(2026, 8, 12, 0, 0));
    }

    private Authentication manageUser() {
        return authenticatedUser("SHAREDFILE:MANAGE");
    }

    private Authentication rootManageUser() {
        return authenticatedUser("SHAREDFILE:ROOT_MANAGE");
    }

    private Authentication noAuthorityUser() {
        return authenticatedUser();
    }

    private Authentication authenticatedUser(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                AUTH_USER, null, List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList());
    }

    @EnableMethodSecurity
    static class MethodSecurityConfiguration {
    }
}

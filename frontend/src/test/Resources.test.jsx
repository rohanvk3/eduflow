import { beforeEach, expect, it, vi } from "vitest";
import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Routes, Route } from "react-router-dom";
import ResourcesPage from "../pages/ResourcesPage";
import ResourceFormPage from "../pages/ResourceFormPage";
import ResourceDetailsPage from "../pages/ResourceDetailsPage";
import ResourceCategoriesPage from "../pages/ResourceCategoriesPage";
import ResourceFoldersPage from "../pages/ResourceFoldersPage";
import { MaterialActions, ResourceFilePicker } from "../components/ResourceUI";
const { api, categories, folders, auth } = vi.hoisted(() => ({
  api: Object.fromEntries(
    [
      "getMaterials",
      "getMaterial",
      "getResourceStats",
      "getResourceOptions",
      "getResourcePolicy",
      "saveMaterial",
      "publishMaterial",
      "archiveMaterial",
      "getMaterialVersions",
      "uploadMaterialVersion",
      "downloadMaterial",
      "previewMaterial",
      "saveDownload",
    ].map((k) => [k, vi.fn()]),
  ),
  categories: {
    getResourceCategories: vi.fn(),
    saveResourceCategory: vi.fn(),
    deactivateResourceCategory: vi.fn(),
  },
  folders: {
    getResourceFolders: vi.fn(),
    getResourceFolder: vi.fn(),
    saveResourceFolder: vi.fn(),
    deleteResourceFolder: vi.fn(),
  },
  auth: { user: { role: "ADMIN", userId: 1 } },
}));
vi.mock("../auth/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../services/resourceService", () => ({
  ...api,
  resourceError: async (e) => e.message || "Request failed",
}));
vi.mock("../services/resourceCategoryService", () => categories);
vi.mock("../services/resourceFolderService", () => folders);
vi.mock("../components/RecordSelect", () => ({
  default: ({ label, value, onChange, required }) => (
    <label>
      {label}
      <input
        aria-label={label}
        value={value || ""}
        required={required}
        onChange={(e) => onChange(e.target.value)}
      />
    </label>
  ),
}));
const material = {
  id: 1,
  title: "Physics notes",
  description: "Chapter one",
  courseId: 2,
  courseName: "Physics",
  batchId: 3,
  batchName: "Morning",
  categoryId: 4,
  categoryName: "Lecture Notes",
  uploadedBy: "Teacher",
  resourceType: "DOCUMENT",
  visibility: "PUBLISHED",
  currentVersion: 1,
  publishedVersion: 1,
  version: 2,
  createdAt: "2026-11-10T10:00:00",
  file: {
    id: 10,
    versionNumber: 1,
    originalFileName: "notes.pdf",
    contentType: "application/pdf",
    fileSize: 100,
  },
};
const page = {
  content: [material],
  page: 0,
  size: 12,
  totalElements: 1,
  totalPages: 1,
};
const folder = {
  id: 8,
  name: "Chapter one",
  courseId: 2,
  courseName: "Physics",
  batchId: 3,
  batchName: "Morning",
  resourceCount: 1,
};
beforeEach(() => {
  vi.resetAllMocks();
  auth.user = { role: "ADMIN", userId: 1 };
  api.getMaterials.mockResolvedValue({ data: page });
  api.getMaterial.mockResolvedValue({ data: material });
  api.getResourceStats.mockResolvedValue({
    data: {
      totalMaterials: 1,
      publishedMaterials: 1,
      draftMaterials: 0,
      uploadedByMe: 1,
      recent: [material],
      courses: [{ id: 2, name: "Physics", count: 1 }],
      categories: [],
    },
  });
  api.getResourceOptions.mockResolvedValue({
    data: {
      courses: [{ id: 2, name: "Physics" }],
      batches: [{ id: 3, name: "Morning", courseId: 2 }],
    },
  });
  api.getResourcePolicy.mockResolvedValue({
    data: {
      extensions: ["pdf", "docx", "pptx", "xlsx", "txt", "png", "jpg"],
      maxFileBytes: 1000,
    },
  });
  api.getMaterialVersions.mockResolvedValue({
    data: [
      {
        ...material.file,
        uploadedBy: "Teacher",
        createdAt: material.createdAt,
      },
    ],
  });
  categories.getResourceCategories.mockResolvedValue({
    data: {
      ...page,
      content: [{ id: 4, name: "Lecture Notes", status: "ACTIVE" }],
    },
  });
  folders.getResourceFolders.mockResolvedValue({
    data: { ...page, content: [folder] },
  });
  folders.getResourceFolder.mockResolvedValue({ data: folder });
  vi.spyOn(window, "confirm").mockReturnValue(true);
  URL.createObjectURL = vi.fn(() => "blob:private-preview");
  URL.revokeObjectURL = vi.fn();
});
const mount = (component, path = "/resources", route = "*") =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={route} element={component} />
        <Route
          path="/resources/materials/9"
          element={<p>Saved material detail</p>}
        />
      </Routes>
    </MemoryRouter>,
  );
it("loads actual resource cards and switches to list view", async () => {
  mount(<ResourcesPage />);
  expect(await screen.findByText("Physics notes")).toBeInTheDocument();
  await userEvent.click(screen.getByLabelText("List view"));
  expect(await screen.findByRole("table")).toBeInTheDocument();
  expect(api.getMaterials).toHaveBeenCalled();
});
it("sends search, course, batch and type filters", async () => {
  mount(<ResourcesPage />);
  await screen.findByText("Physics notes");
  fireEvent.change(screen.getByLabelText("Search resources"), {
    target: { value: "chapter" },
  });
  fireEvent.change(screen.getByLabelText("Course"), { target: { value: "2" } });
  fireEvent.change(screen.getByLabelText("Batch"), { target: { value: "3" } });
  fireEvent.change(screen.getByLabelText("Resource type"), {
    target: { value: "DOCUMENT" },
  });
  await waitFor(() =>
    expect(api.getMaterials).toHaveBeenLastCalledWith(
      expect.objectContaining({
        search: "chapter",
        courseId: "2",
        batchId: "3",
        resourceType: "DOCUMENT",
      }),
      expect.anything(),
    ),
  );
});
it("displays backend failure with retry", async () => {
  api.getMaterials.mockRejectedValueOnce({
    response: { data: { message: "Access expired" } },
  });
  mount(<ResourcesPage />);
  expect(await screen.findByRole("alert")).toHaveTextContent("Access expired");
  await userEvent.click(screen.getByText("Retry"));
  expect(await screen.findByText("Physics notes")).toBeInTheDocument();
});
it("shows useful empty and loading states", async () => {
  api.getMaterials.mockResolvedValue({
    data: { ...page, content: [], totalElements: 0 },
  });
  mount(<ResourcesPage />);
  expect(
    await screen.findByText("No learning resources found"),
  ).toBeInTheDocument();
});
it("student portal hides management actions and parent does not fetch resources", async () => {
  auth.user.role = "STUDENT";
  const first = mount(<ResourcesPage own />);
  expect(
    await screen.findByRole("heading", { name: "My Resources" }),
  ).toBeInTheDocument();
  expect(screen.queryByText("Upload material")).not.toBeInTheDocument();
  first.unmount();
  api.getMaterials.mockClear();
  auth.user.role = "PARENT";
  mount(<ResourcesPage />);
  expect(screen.getByText("Resource access restricted")).toBeInTheDocument();
  expect(api.getMaterials).not.toHaveBeenCalled();
});
it("uses authenticated blob preview and revokes object URL on close", async () => {
  api.previewMaterial.mockResolvedValue({
    data: new Blob(["pdf"], { type: "application/pdf" }),
  });
  mount(<MaterialActions material={material} />);
  await userEvent.click(screen.getByText("Preview"));
  expect(await screen.findByTitle("Physics notes PDF preview")).toHaveAttribute(
    "src",
    "blob:private-preview",
  );
  expect(api.previewMaterial).toHaveBeenCalledWith(1);
  await userEvent.click(screen.getByLabelText("Close dialog"));
  expect(URL.revokeObjectURL).toHaveBeenCalledWith("blob:private-preview");
});
it("downloads files with the authenticated service", async () => {
  const blob = new Blob(["pdf"]);
  api.downloadMaterial.mockResolvedValue({ data: blob });
  mount(<MaterialActions material={material} />);
  await userEvent.click(screen.getByText("Download"));
  await waitFor(() =>
    expect(api.saveDownload).toHaveBeenCalledWith(blob, "notes.pdf"),
  );
});
it("rejects unsupported and oversized file selections", async () => {
  mount(<ResourceFilePicker onChange={vi.fn()} />);
  await waitFor(() =>
    expect(screen.getByLabelText("Resource file")).toBeEnabled(),
  );
  fireEvent.change(screen.getByLabelText("Resource file"), {
    target: { files: [new File(["x"], "run.exe")] },
  });
  expect(await screen.findByRole("alert")).toHaveTextContent("supported");
  fireEvent.change(screen.getByLabelText("Resource file"), {
    target: { files: [new File(["x".repeat(1001)], "notes.pdf")] },
  });
  expect(await screen.findByRole("alert")).toHaveTextContent("exceeds");
});
it("creates a real link draft and navigates after server confirmation", async () => {
  api.saveMaterial.mockResolvedValue({
    data: { ...material, id: 9, visibility: "DRAFT" },
  });
  mount(<ResourceFormPage />, "/resources/new");
  await screen.findByText("Save draft");
  await screen.findByRole("option", { name: "Physics" });
  fireEvent.change(screen.getByLabelText("Title *"), {
    target: { value: "Lecture link" },
  });
  fireEvent.change(screen.getByLabelText("Course"), { target: { value: "2" } });
  fireEvent.change(screen.getByLabelText("Category"), {
    target: { value: "4" },
  });
  fireEvent.change(screen.getByLabelText("Resource type *"), {
    target: { value: "LINK" },
  });
  fireEvent.change(screen.getByLabelText("External URL *"), {
    target: { value: "https://example.org/lecture" },
  });
  await userEvent.click(screen.getByText("Save draft"));
  expect(await screen.findByText("Saved material detail")).toBeInTheDocument();
  expect(api.saveMaterial).toHaveBeenCalledWith(
    undefined,
    expect.objectContaining({
      courseId: 2,
      categoryId: 4,
      resourceType: "LINK",
      externalUrl: "https://example.org/lecture",
    }),
    null,
    expect.any(Function),
  );
});
it("shows version history and publishes an explicit staged version", async () => {
  api.getMaterial.mockResolvedValue({
    data: { ...material, currentVersion: 2, version: 3 },
  });
  api.publishMaterial.mockResolvedValue({ data: material });
  mount(
    <ResourceDetailsPage />,
    "/resources/materials/1",
    "/resources/materials/:id",
  );
  expect(await screen.findByText("Version history")).toBeInTheDocument();
  expect(await screen.findByText("Download v1")).toBeInTheDocument();
  await userEvent.click(screen.getByText("Publish latest version"));
  await waitFor(() => expect(api.publishMaterial).toHaveBeenCalledWith(1, 3));
});
it("student details never loads historical staff versions", async () => {
  auth.user.role = "STUDENT";
  mount(
    <ResourceDetailsPage />,
    "/resources/materials/1",
    "/resources/materials/:id",
  );
  expect(await screen.findByText("Resource information")).toBeInTheDocument();
  expect(api.getMaterialVersions).not.toHaveBeenCalled();
  expect(screen.queryByText("Archive")).not.toBeInTheDocument();
});
it("folder explorer navigates nested folders and forwards scope", async () => {
  mount(<ResourceFoldersPage />);
  await userEvent.click(await screen.findByText("Chapter one"));
  await waitFor(() =>
    expect(folders.getResourceFolders).toHaveBeenLastCalledWith(
      expect.objectContaining({ parentFolderId: "8", rootOnly: false }),
      expect.anything(),
    ),
  );
  expect(await screen.findByLabelText("Folder breadcrumb")).toHaveTextContent(
    "Chapter one",
  );
});
it("surfaces nonempty folder deletion conflicts", async () => {
  folders.deleteResourceFolder.mockRejectedValue(
    new Error("Move resources before deleting"),
  );
  mount(<ResourceFoldersPage />);
  await userEvent.click(await screen.findByText("Delete"));
  expect(await screen.findByRole("alert")).toHaveTextContent("Move resources");
});
it("admin creates categories through API", async () => {
  categories.saveResourceCategory.mockResolvedValue({ data: { id: 5 } });
  mount(<ResourceCategoriesPage />);
  await userEvent.click(screen.getByText("Add category"));
  await userEvent.type(screen.getByLabelText("Name *"), "Practice");
  await userEvent.click(screen.getByText("Save category"));
  await waitFor(() =>
    expect(categories.saveResourceCategory).toHaveBeenCalledWith(
      undefined,
      expect.objectContaining({ name: "Practice" }),
    ),
  );
});
it("keeps external links isolated from the application opener", () => {
  mount(
    <MaterialActions
      material={{
        ...material,
        resourceType: "LINK",
        externalUrl: "https://example.org",
      }}
    />,
  );
  expect(screen.getByRole("link")).toHaveAttribute(
    "rel",
    "noopener noreferrer",
  );
  expect(screen.getByRole("link")).toHaveAttribute("target", "_blank");
});
it("filters teacher My Resources using the restored login ID", async () => {
  auth.user = { role: "TEACHER", id: 12 };
  mount(<ResourcesPage own />);
  await waitFor(() =>
    expect(api.getMaterials).toHaveBeenCalledWith(
      expect.objectContaining({ uploadedBy: 12 }),
      expect.anything(),
    ),
  );
});
it("uploads a selected replacement as a new version", async () => {
  api.uploadMaterialVersion.mockResolvedValue({ data: material });
  mount(
    <ResourceDetailsPage />,
    "/resources/materials/1",
    "/resources/materials/:id",
  );
  await waitFor(() =>
    expect(screen.getByLabelText("Resource file")).toBeEnabled(),
  );
  const upload = new File(["new notes"], "notes.txt", { type: "text/plain" });
  fireEvent.change(screen.getByLabelText("Resource file"), {
    target: { files: [upload] },
  });
  fireEvent.change(screen.getByLabelText("Change notes"), {
    target: { value: "Added examples" },
  });
  await userEvent.click(screen.getByText("Save new version"));
  await waitFor(() =>
    expect(api.uploadMaterialVersion).toHaveBeenCalledWith(
      1,
      2,
      upload,
      "Added examples",
      expect.any(Function),
    ),
  );
});
it("creates a material and its file in the same request", async () => {
  api.saveMaterial.mockResolvedValue({
    data: { ...material, id: 9, visibility: "DRAFT" },
  });
  mount(<ResourceFormPage />, "/resources/new");
  await waitFor(() =>
    expect(screen.getByLabelText("Resource file")).toBeEnabled(),
  );
  fireEvent.change(screen.getByLabelText("Title *"), {
    target: { value: "Uploaded notes" },
  });
  fireEvent.change(screen.getByLabelText("Course"), { target: { value: "2" } });
  fireEvent.change(screen.getByLabelText("Category"), {
    target: { value: "4" },
  });
  const file = new File(["pdf content"], "notes.pdf", {
    type: "application/pdf",
  });
  fireEvent.change(screen.getByLabelText("Resource file"), {
    target: { files: [file] },
  });
  await userEvent.click(screen.getByText("Save draft"));
  expect(await screen.findByText("Saved material detail")).toBeInTheDocument();
  expect(api.saveMaterial).toHaveBeenCalledWith(
    undefined,
    expect.objectContaining({ title: "Uploaded notes" }),
    file,
    expect.any(Function),
  );
});

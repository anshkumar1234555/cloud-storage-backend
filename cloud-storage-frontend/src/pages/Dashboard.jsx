import { useEffect, useState } from "react";
import api from "../services/api";
import Sidebar from "../components/Sidebar";

function Dashboard() {
  // =========================================================
  // STATES
  // =========================================================

  const [files, setFiles] = useState([]);
  const [folders, setFolders] = useState([]);
  const [sharedFiles, setSharedFiles] = useState([]);
  const [trashFiles, setTrashFiles] = useState([]);

  const [page, setPage] = useState("drive");

  const [selectedFile, setSelectedFile] = useState(null);
  const [uploading, setUploading] = useState(false);
  const [downloadingId, setDownloadingId] = useState(null);

  // =========================================================
  // FOLDER NAVIGATION
  // =========================================================

  const [currentFolderId, setCurrentFolderId] = useState(null);
  const [currentFolderName, setCurrentFolderName] =
    useState("My Drive");

  const [folderPath, setFolderPath] = useState([]);

  // =========================================================
  // CREATE FOLDER
  // =========================================================

  const [newFolderName, setNewFolderName] = useState("");
  const [creatingFolder, setCreatingFolder] = useState(false);

  // =========================================================
  // SEARCH
  // =========================================================

  const [searchName, setSearchName] = useState("");
  const [searching, setSearching] = useState(false);

  // =========================================================
  // RENAME
  // =========================================================

  const [renamingId, setRenamingId] = useState(null);
  const [renameName, setRenameName] = useState("");

  // =========================================================
  // LOAD ROOT FILES
  // =========================================================

  const loadFiles = async () => {
    try {
      const response = await api.get("/files");
      setFiles(response.data);
    } catch (error) {
      console.error("Failed to load files:", error);
    }
  };

  // =========================================================
  // LOAD ROOT FOLDERS
  // =========================================================

  const loadFolders = async () => {
    try {
      const response = await api.get("/folders");
      setFolders(response.data);
    } catch (error) {
      console.error("Failed to load folders:", error);
    }
  };

  // =========================================================
  // LOAD CHILD FOLDERS
  // =========================================================

  const loadChildFolders = async (folderId) => {
    try {
      const response = await api.get(`/folders/${folderId}`);
      setFolders(response.data);
    } catch (error) {
      console.error("Failed to load child folders:", error);
    }
  };

  // =========================================================
  // LOAD FILES INSIDE FOLDER
  // =========================================================

  const loadFolderFiles = async (folderId) => {
    try {
      const response = await api.get(`/files?folderId=${folderId}`);
      setFiles(response.data);
    } catch (error) {
      console.error("Failed to load folder files:", error);
    }
  };

  // =========================================================
  // OPEN FOLDER
  // =========================================================

  const openFolder = async (folder) => {
    try {
      setCurrentFolderId(folder.id);
      setCurrentFolderName(folder.name);

      setFolderPath((previousPath) => [
        ...previousPath,
        {
          id: folder.id,
          name: folder.name,
        },
      ]);

      await loadChildFolders(folder.id);
      await loadFolderFiles(folder.id);
    } catch (error) {
      console.error("Failed to open folder:", error);
    }
  };

  // =========================================================
  // GO BACK
  // =========================================================

  const goBack = async () => {
    const newPath = [...folderPath];

    newPath.pop();

    setFolderPath(newPath);

    // Go back to My Drive
    if (newPath.length === 0) {
      setCurrentFolderId(null);
      setCurrentFolderName("My Drive");

      await loadFolders();
      await loadFiles();

      return;
    }

    // Go back to parent folder
    const parentFolder = newPath[newPath.length - 1];

    setCurrentFolderId(parentFolder.id);
    setCurrentFolderName(parentFolder.name);

    await loadChildFolders(parentFolder.id);
    await loadFolderFiles(parentFolder.id);
  };

  // =========================================================
  // CREATE FOLDER
  // =========================================================

  const handleCreateFolder = async () => {
    const name = newFolderName.trim();

    if (!name) {
      alert("Please enter folder name");
      return;
    }

    try {
      setCreatingFolder(true);

      await api.post("/folders", {
        name: name,
        parentId: currentFolderId,
      });

      alert("Folder created successfully");

      setNewFolderName("");

      if (currentFolderId === null) {
        await loadFolders();
      } else {
        await loadChildFolders(currentFolderId);
      }
    } catch (error) {
      console.error("Create folder failed:", error);

      alert(
        error.response?.data?.message ||
          "Failed to create folder"
      );
    } finally {
      setCreatingFolder(false);
    }
  };

  // =========================================================
  // UPLOAD FILE
  // =========================================================

  const handleUpload = async () => {
    if (!selectedFile) {
      alert("Please select a file first");
      return;
    }

    try {
      setUploading(true);

      const formData = new FormData();

      formData.append("file", selectedFile);

      if (currentFolderId !== null) {
        formData.append("folderId", currentFolderId);
      }

      await api.post("/files/upload", formData);

      alert("File uploaded successfully");

      setSelectedFile(null);

      if (currentFolderId === null) {
        await loadFiles();
      } else {
        await loadFolderFiles(currentFolderId);
      }
    } catch (error) {
      console.error("Upload failed:", error);

      alert(
        error.response?.data?.message ||
          "File upload failed"
      );
    } finally {
      setUploading(false);
    }
  };

  // =========================================================
  // DOWNLOAD FILE
  // =========================================================

  const handleDownload = async (fileId, fileName) => {
    try {
      setDownloadingId(fileId);

      const response = await api.get(
        `/files/${fileId}/download`,
        {
          responseType: "blob",
        }
      );

      const blob = new Blob([response.data], {
        type:
          response.headers["content-type"] ||
          "application/octet-stream",
      });

      const url = window.URL.createObjectURL(blob);

      const link = document.createElement("a");

      link.href = url;
      link.download = fileName || "file";

      document.body.appendChild(link);
      link.click();
      link.remove();

      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error("Download failed:", error);

      alert("Failed to download file");
    } finally {
      setDownloadingId(null);
    }
  };

  // =========================================================
  // MOVE FILE TO TRASH
  // =========================================================

  const handleTrash = async (fileId) => {
    try {
      await api.delete(`/files/${fileId}`);

      alert("File moved to Trash");

      if (currentFolderId === null) {
        await loadFiles();
      } else {
        await loadFolderFiles(currentFolderId);
      }

      await loadTrash();
    } catch (error) {
      console.error("Trash failed:", error);

      alert(
        error.response?.data?.message ||
          "Failed to move file to Trash"
      );
    }
  };

  // =========================================================
  // LOAD TRASH
  // =========================================================

  const loadTrash = async () => {
    try {
      const response = await api.get("/files/trash");

      setTrashFiles(response.data);
    } catch (error) {
      console.error("Failed to load trash:", error);
    }
  };

  // =========================================================
  // RESTORE FILE
  // =========================================================

  const handleRestore = async (fileId) => {
    try {
      await api.put(`/files/${fileId}/restore`);

      alert("File restored");

      await loadTrash();

      if (currentFolderId === null) {
        await loadFiles();
      } else {
        await loadFolderFiles(currentFolderId);
      }
    } catch (error) {
      console.error("Restore failed:", error);

      alert(
        error.response?.data?.message ||
          "Failed to restore file"
      );
    }
  };

  // =========================================================
  // PERMANENT DELETE
  // =========================================================

  const handlePermanentDelete = async (fileId) => {
    const confirmed = window.confirm(
      "Are you sure you want to permanently delete this file? This cannot be undone."
    );

    if (!confirmed) {
      return;
    }

    try {
      await api.delete(`/files/${fileId}/permanent`);

      alert("File permanently deleted");

      await loadTrash();
    } catch (error) {
      console.error(
        "Permanent delete failed:",
        error
      );

      alert(
        error.response?.data?.message ||
          "Failed to permanently delete file"
      );
    }
  };

  // =========================================================
  // RENAME FILE
  // =========================================================

  const startRename = (file) => {
    setRenamingId(file.id);
    setRenameName(file.name);
  };

  const cancelRename = () => {
    setRenamingId(null);
    setRenameName("");
  };

  const handleRename = async (fileId) => {
    const name = renameName.trim();

    if (!name) {
      alert("Please enter a file name");
      return;
    }

    try {
      await api.put(`/files/${fileId}/rename`, {
        name: name,
      });

      alert("File renamed successfully");

      cancelRename();

      if (currentFolderId === null) {
        await loadFiles();
      } else {
        await loadFolderFiles(currentFolderId);
      }
    } catch (error) {
      console.error("Rename failed:", error);

      alert(
        error.response?.data?.message ||
          "Failed to rename file"
      );
    }
  };

  // =========================================================
  // MOVE FILE
  // =========================================================

  const handleMoveFile = async (file) => {
    let folderId = window.prompt(
      "Enter destination folder ID.\n\nEnter 0 to move the file to My Drive:"
    );

    if (folderId === null) {
      return;
    }

    folderId = folderId.trim();

    if (!folderId) {
      return;
    }

    if (folderId === "0") {
      folderId = null;
    } else {
      folderId = Number(folderId);

      if (!Number.isInteger(folderId) || folderId <= 0) {
        alert("Invalid folder ID");
        return;
      }
    }

    try {
      await api.put(`/files/${file.id}/move`, {
        folderId: folderId,
      });

      alert("File moved successfully");

      if (currentFolderId === null) {
        await loadFiles();
      } else {
        await loadFolderFiles(currentFolderId);
      }
    } catch (error) {
      console.error("Move failed:", error);

      alert(
        error.response?.data?.message ||
          "Failed to move file"
      );
    }
  };

  // =========================================================
  // SEARCH FILES
  // =========================================================

  const handleSearch = async () => {
    const name = searchName.trim();

    if (!name) {
      if (currentFolderId === null) {
        await loadFiles();
      } else {
        await loadFolderFiles(currentFolderId);
      }

      return;
    }

    try {
      setSearching(true);

      const response = await api.get(
        `/files/search?name=${encodeURIComponent(name)}`
      );

      setFiles(response.data);
      setPage("drive");
    } catch (error) {
      console.error("Search failed:", error);

      alert(
        error.response?.data?.message ||
          "Search failed"
      );
    } finally {
      setSearching(false);
    }
  };

  // =========================================================
  // LOAD SHARED FILES
  // =========================================================

  const loadSharedFiles = async () => {
    try {
      const response = await api.get(
        "/files/shared-with-me"
      );

      setSharedFiles(response.data);
    } catch (error) {
      console.error(
        "Failed to load shared files:",
        error
      );
    }
  };

  // =========================================================
  // DOWNLOAD SHARED FILE
  // =========================================================

  const handleSharedDownload = async (
    fileId,
    fileName
  ) => {
    try {
      setDownloadingId(fileId);

      const response = await api.get(
        `/files/${fileId}/shared-download`,
        {
          responseType: "blob",
        }
      );

      const blob = new Blob([response.data], {
        type:
          response.headers["content-type"] ||
          "application/octet-stream",
      });

      const url = window.URL.createObjectURL(blob);

      const link = document.createElement("a");

      link.href = url;
      link.download = fileName || "shared-file";

      document.body.appendChild(link);
      link.click();
      link.remove();

      window.URL.revokeObjectURL(url);
    } catch (error) {
      console.error(
        "Shared download failed:",
        error
      );

      alert("Failed to download shared file");
    } finally {
      setDownloadingId(null);
    }
  };

  // =========================================================
  // LOGOUT
  // =========================================================

  const handleLogout = () => {
    localStorage.removeItem("token");

    window.location.href = "/";
  };

  // =========================================================
  // PAGE CHANGE
  // =========================================================

  const handlePageChange = (newPage) => {
    setPage(newPage);

    if (newPage === "shared") {
      loadSharedFiles();
    }

    if (newPage === "trash") {
      loadTrash();
    }

    if (newPage === "drive") {
      setCurrentFolderId(null);
      setCurrentFolderName("My Drive");
      setFolderPath([]);

      loadFiles();
      loadFolders();
    }
  };

  // =========================================================
  // INITIAL LOAD
  // =========================================================

  useEffect(() => {
    loadFiles();
    loadFolders();
  }, []);

  // =========================================================
  // UI
  // =========================================================

  return (
    <div
      style={{
        display: "flex",
        minHeight: "100vh",
        fontFamily: "Arial, sans-serif",
      }}
    >
      {/* =====================================================
          SIDEBAR
      ===================================================== */}

      <Sidebar
        page={page}
        setPage={handlePageChange}
        onLogout={handleLogout}
      />

      {/* =====================================================
          MAIN CONTENT
      ===================================================== */}

      <main
        style={{
          flex: 1,
          padding: "30px",
          overflowX: "auto",
        }}
      >
        {/* ===================================================
            MY DRIVE
        =================================================== */}

        {page === "drive" && (
          <div>
            <h1>{currentFolderName}</h1>

            {/* =================================================
                SEARCH
            ================================================= */}

            <div
              style={{
                display: "flex",
                gap: "10px",
                marginBottom: "25px",
              }}
            >
              <input
                type="text"
                placeholder="Search files..."
                value={searchName}
                onChange={(e) =>
                  setSearchName(e.target.value)
                }
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    handleSearch();
                  }
                }}
                style={{
                  padding: "10px",
                  width: "300px",
                }}
              />

              <button
                type="button"
                onClick={handleSearch}
                disabled={searching}
              >
                {searching ? "Searching..." : "🔍 Search"}
              </button>

              <button
                type="button"
                onClick={async () => {
                  setSearchName("");

                  if (currentFolderId === null) {
                    await loadFiles();
                  } else {
                    await loadFolderFiles(
                      currentFolderId
                    );
                  }
                }}
              >
                Clear
              </button>
            </div>

            {/* =================================================
                BREADCRUMB
            ================================================= */}

            {folderPath.length > 0 && (
              <div
                style={{
                  marginBottom: "20px",
                }}
              >
                <button
                  type="button"
                  onClick={goBack}
                >
                  ← Back
                </button>

                <span
                  style={{
                    marginLeft: "15px",
                  }}
                >
                  My Drive /{" "}
                  {folderPath
                    .map((folder) => folder.name)
                    .join(" / ")}
                </span>
              </div>
            )}

            {/* =================================================
                CREATE FOLDER
            ================================================= */}

            <div
              style={{
                marginBottom: "25px",
                padding: "15px",
                border: "1px solid #ddd",
                borderRadius: "8px",
              }}
            >
              <h3>Create Folder</h3>

              <input
                type="text"
                placeholder="Folder name"
                value={newFolderName}
                onChange={(e) =>
                  setNewFolderName(e.target.value)
                }
                style={{
                  padding: "10px",
                }}
              />

              <button
                type="button"
                onClick={handleCreateFolder}
                disabled={creatingFolder}
                style={{
                  marginLeft: "10px",
                  padding: "10px 15px",
                }}
              >
                {creatingFolder
                  ? "Creating..."
                  : "Create Folder"}
              </button>
            </div>

            {/* =================================================
                UPLOAD
            ================================================= */}

            <div
              style={{
                marginBottom: "30px",
                padding: "15px",
                border: "1px solid #ddd",
                borderRadius: "8px",
              }}
            >
              <h3>Upload File</h3>

              <input
                type="file"
                onChange={(e) =>
                  setSelectedFile(e.target.files[0])
                }
              />

              <button
                type="button"
                onClick={handleUpload}
                disabled={uploading}
                style={{
                  marginLeft: "10px",
                  padding: "10px 15px",
                }}
              >
                {uploading ? "Uploading..." : "Upload File"}
              </button>

              {selectedFile && (
                <p>
                  Selected:{" "}
                  <strong>{selectedFile.name}</strong>
                </p>
              )}
            </div>

            {/* =================================================
                FOLDERS
            ================================================= */}

            <h2>Folders</h2>

            {folders.length === 0 ? (
              <p>No folders found.</p>
            ) : (
              folders.map((folder) => (
                <div
                  key={folder.id}
                  onDoubleClick={() => openFolder(folder)}
                  style={{
                    border: "1px solid #ddd",
                    padding: "15px",
                    marginBottom: "10px",
                    borderRadius: "8px",
                    cursor: "pointer",
                  }}
                >
                  📁 <strong>{folder.name}</strong>

                  <span
                    style={{
                      marginLeft: "15px",
                      color: "#777",
                      fontSize: "13px",
                    }}
                  >
                    ID: {folder.id}
                  </span>

                  <div
                    style={{
                      fontSize: "12px",
                      color: "#666",
                      marginTop: "5px",
                    }}
                  >
                    Double-click to open
                  </div>
                </div>
              ))
            )}

            {/* =================================================
                FILES
            ================================================= */}

            <h2
              style={{
                marginTop: "30px",
              }}
            >
              Files
            </h2>

            {files.length === 0 ? (
              <p>No files found.</p>
            ) : (
              files.map((file) => (
                <div
                  key={file.id}
                  style={{
                    border: "1px solid #ddd",
                    padding: "15px",
                    marginBottom: "10px",
                    borderRadius: "8px",
                  }}
                >
                  {/* FILE NAME / RENAME */}

                  {renamingId === file.id ? (
                    <div>
                      <input
                        type="text"
                        value={renameName}
                        onChange={(e) =>
                          setRenameName(e.target.value)
                        }
                        autoFocus
                      />

                      <button
                        type="button"
                        onClick={() =>
                          handleRename(file.id)
                        }
                        style={{
                          marginLeft: "10px",
                        }}
                      >
                        Save
                      </button>

                      <button
                        type="button"
                        onClick={cancelRename}
                        style={{
                          marginLeft: "10px",
                        }}
                      >
                        Cancel
                      </button>
                    </div>
                  ) : (
                    <div>
                      📄 <strong>{file.name}</strong>
                    </div>
                  )}

                  {/* FILE INFO */}

                  <div
                    style={{
                      marginTop: "8px",
                      fontSize: "13px",
                      color: "#666",
                    }}
                  >
                    Size: {file.size} bytes
                  </div>

                  {/* FILE ACTIONS */}

                  <div
                    style={{
                      marginTop: "12px",
                      display: "flex",
                      flexWrap: "wrap",
                      gap: "8px",
                    }}
                  >
                    {/* DOWNLOAD */}

                    <button
                      type="button"
                      onClick={() =>
                        handleDownload(
                          file.id,
                          file.originalName
                        )
                      }
                      disabled={
                        downloadingId === file.id
                      }
                    >
                      {downloadingId === file.id
                        ? "Downloading..."
                        : "⬇️ Download"}
                    </button>

                    {/* RENAME */}

                    <button
                      type="button"
                      onClick={() => startRename(file)}
                    >
                      ✏️ Rename
                    </button>

                    {/* MOVE */}

                    <button
                      type="button"
                      onClick={() =>
                        handleMoveFile(file)
                      }
                    >
                      📂 Move
                    </button>

                    {/* TRASH */}

                    <button
                      type="button"
                      onClick={() =>
                        handleTrash(file.id)
                      }
                    >
                      🗑️ Trash
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* =====================================================
            SHARED WITH ME
        ===================================================== */}

        {page === "shared" && (
          <div>
            <h1>Shared with me</h1>

            {sharedFiles.length === 0 ? (
              <p>No files have been shared with you.</p>
            ) : (
              sharedFiles.map((share) => {
                const sharedFile = share.file;

                if (!sharedFile) {
                  return null;
                }

                return (
                  <div
                    key={share.id}
                    style={{
                      border: "1px solid #ddd",
                      padding: "20px",
                      marginBottom: "15px",
                      borderRadius: "8px",
                    }}
                  >
                    <h3>
                      📄{" "}
                      {sharedFile.name ||
                        sharedFile.originalName}
                    </h3>

                    <p>
                      Owner:{" "}
                      {sharedFile.owner?.name ||
                        sharedFile.owner?.email ||
                        "Unknown"}
                    </p>

                    <p>
                      Permission:{" "}
                      <strong>{share.role}</strong>
                    </p>

                    <button
                      type="button"
                      onClick={() =>
                        handleSharedDownload(
                          sharedFile.id,
                          sharedFile.originalName ||
                            sharedFile.name
                        )
                      }
                      disabled={
                        downloadingId ===
                        sharedFile.id
                      }
                    >
                      {downloadingId ===
                      sharedFile.id
                        ? "Downloading..."
                        : "⬇️ Download"}
                    </button>
                  </div>
                );
              })
            )}
          </div>
        )}

        {/* =====================================================
            TRASH
        ===================================================== */}

        {page === "trash" && (
          <div>
            <h1>Trash</h1>

            {trashFiles.length === 0 ? (
              <p>Trash is empty.</p>
            ) : (
              trashFiles.map((file) => (
                <div
                  key={file.id}
                  style={{
                    border: "1px solid #ddd",
                    padding: "15px",
                    marginBottom: "10px",
                    borderRadius: "8px",
                  }}
                >
                  <h3>🗑️ {file.name}</h3>

                  <div
                    style={{
                      display: "flex",
                      gap: "10px",
                      flexWrap: "wrap",
                    }}
                  >
                    {/* RESTORE */}

                    <button
                      type="button"
                      onClick={() =>
                        handleRestore(file.id)
                      }
                    >
                      ♻️ Restore
                    </button>

                    {/* PERMANENT DELETE */}

                    <button
                      type="button"
                      onClick={() =>
                        handlePermanentDelete(
                          file.id
                        )
                      }
                    >
                      ❌ Permanent Delete
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
      </main>
    </div>
  );
}

export default Dashboard;
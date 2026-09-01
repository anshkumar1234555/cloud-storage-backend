function Sidebar({ page, setPage, onLogout }) {
  return (
    <aside
      style={{
        width: "240px",
        minHeight: "100vh",
        backgroundColor: "#f5f5f5",
        borderRight: "1px solid #ddd",
        padding: "20px",
        boxSizing: "border-box",
      }}
    >
      {/* LOGO */}
      <h2 style={{ marginBottom: "30px" }}>
        ☁️ Cloud Storage
      </h2>

      {/* MY DRIVE */}
      <button
        type="button"
        onClick={() => setPage("drive")}
        style={{
          display: "block",
          width: "100%",
          padding: "12px",
          marginBottom: "10px",
          textAlign: "left",
          cursor: "pointer",
          border: "none",
          borderRadius: "6px",
          backgroundColor:
            page === "drive" ? "#ddd" : "transparent",
          fontSize: "16px",
        }}
      >
        📁 My Drive
      </button>

      {/* SHARED WITH ME */}
      <button
        type="button"
        onClick={() => setPage("shared")}
        style={{
          display: "block",
          width: "100%",
          padding: "12px",
          marginBottom: "10px",
          textAlign: "left",
          cursor: "pointer",
          border: "none",
          borderRadius: "6px",
          backgroundColor:
            page === "shared" ? "#ddd" : "transparent",
          fontSize: "16px",
        }}
      >
        🤝 Shared with me
      </button>

      {/* TRASH */}
      <button
        type="button"
        onClick={() => setPage("trash")}
        style={{
          display: "block",
          width: "100%",
          padding: "12px",
          marginBottom: "10px",
          textAlign: "left",
          cursor: "pointer",
          border: "none",
          borderRadius: "6px",
          backgroundColor:
            page === "trash" ? "#ddd" : "transparent",
          fontSize: "16px",
        }}
      >
        🗑️ Trash
      </button>

      {/* LOGOUT */}
      <button
        type="button"
        onClick={onLogout}
        style={{
          display: "block",
          width: "100%",
          padding: "12px",
          marginTop: "30px",
          textAlign: "left",
          cursor: "pointer",
          border: "none",
          borderRadius: "6px",
          backgroundColor: "transparent",
          fontSize: "16px",
        }}
      >
        🚪 Logout
      </button>
    </aside>
  );
}

export default Sidebar;
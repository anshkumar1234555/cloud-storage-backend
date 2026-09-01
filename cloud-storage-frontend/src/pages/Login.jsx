import { useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../services/api";

function Login() {
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const navigate = useNavigate();

  const handleLogin = async () => {
    try {
      const response = await api.post("/auth/login", {
        email,
        password,
      });

      console.log("LOGIN RESPONSE:", response.data);

     const token =
  	typeof response.data === "string"
    	? response.data
    	: response.data.token ||
     	 response.data.jwt ||
      	 response.data.accessToken;

     	 if (!token) {
        alert("Login worked, but JWT token was not found.");
        return;
      }

      localStorage.setItem("token", token);

      alert("Login successful!");

      navigate("/dashboard");

    } catch (error) {
      console.error("LOGIN ERROR:", error);

      if (error.response) {
        alert(
          "Login failed: " +
          (error.response.data?.message || error.response.status)
        );
      } else {
        alert("Cannot connect to backend.");
      }
    }
  };

  return (
    <div>
      <h1>Cloud Storage</h1>

      <input
        type="email"
        placeholder="Email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
      />

      <br />
      <br />

      <input
        type="password"
        placeholder="Password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
      />

      <br />
      <br />

      <button onClick={handleLogin}>
        Login
      </button>
    </div>
  );
}

export default Login;
import { createContext, useContext, useEffect, useState } from "react";
import { api, TOKEN_KEY } from "../api/client";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    let active = true;
    const expired = () => {
      setUser(null);
      setNotice("Your session has expired. Please sign in again.");
    };
    window.addEventListener("eduflow:unauthorized", expired);
    const restore = async () => {
      try {
        if (sessionStorage.getItem(TOKEN_KEY)) {
          const { data } = await api.get("/auth/me");
          if (active) setUser(data);
        }
      } catch {
        if (active) setNotice("Please sign in to continue.");
      } finally {
        if (active) setLoading(false);
      }
    };
    restore();
    return () => {
      active = false;
      window.removeEventListener("eduflow:unauthorized", expired);
    };
  }, []);

  const login = async (email, password) => {
    sessionStorage.removeItem(TOKEN_KEY);
    const { data } = await api.post("/auth/login", { email, password });
    sessionStorage.setItem(TOKEN_KEY, data.token);
    setUser({ ...data, id: data.userId });
    setNotice("");
  };
  const logout = (reason = "") => {
    sessionStorage.removeItem(TOKEN_KEY);
    setUser(null);
    setNotice(typeof reason === "string" ? reason : "");
  };
  const refreshUser = async () => {
    const { data } = await api.get("/auth/me");
    setUser(data);
  };
  return (
    <AuthContext.Provider
      value={{ user, loading, notice, login, logout, refreshUser }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => useContext(AuthContext);

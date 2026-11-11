import { useEffect, useState } from "react";
import { api } from "../api/client";
export default function InstituteBrand({ compact = false }) {
  const [name, setName] = useState("EduFlow"),
    [image, setImage] = useState(""),
    [revision, setRevision] = useState(0);
  useEffect(() => {
    const refresh = () => setRevision((v) => v + 1);
    window.addEventListener("eduflow:branding-changed", refresh);
    return () =>
      window.removeEventListener("eduflow:branding-changed", refresh);
  }, []);
  useEffect(() => {
    const c = new AbortController();
    let url;
    api
      .get("/institute/branding", { signal: c.signal })
      .then(async ({ data }) => {
        if (c.signal.aborted) return;
        setName(data.instituteName);
        if (data.hasLogo) {
          const r = await api.get("/institute/logo", {
            responseType: "blob",
            signal: c.signal,
          });
          if (c.signal.aborted) return;
          url = URL.createObjectURL(r.data);
          setImage(url);
        } else setImage("");
      })
      .catch(() => {});
    return () => {
      c.abort();
      if (url) URL.revokeObjectURL(url);
    };
  }, [revision]);
  return (
    <>
      {image && (
        <img
          className="h-9 w-9 shrink-0 rounded-lg object-contain"
          src={image}
          alt="Institute logo"
        />
      )}
      {!compact && (
        <span
          className="block min-w-0 max-w-36 truncate text-xl font-semibold tracking-tight text-slate-900"
          title={name}
        >
          {name}
        </span>
      )}
    </>
  );
}

import React from "react";

interface FormFieldProps {
  label: string;
  children: React.ReactNode;
  error?: string;
  hint?: string;
}

export function FormField({ label, children, error, hint }: FormFieldProps) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: 4 }}>
      <label style={{ fontSize: 13, fontWeight: 600, color: "var(--color-text)" }}>
        {label}
      </label>
      {children}
      {hint && <p style={{ margin: 0, fontSize: 12, color: "var(--color-text-muted)" }}>{hint}</p>}
      {error && <p style={{ margin: 0, fontSize: 12, color: "var(--color-danger)" }}>{error}</p>}
    </div>
  );
}

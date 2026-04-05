import React from "react";

type StatusVariant = "error" | "success" | "warning" | "info";

const statusStyles: Record<StatusVariant, React.CSSProperties> = {
  error:   { background: "var(--color-danger-light)",  color: "var(--color-danger)",   border: "1px solid #fca5a5" },
  success: { background: "var(--color-success-light)", color: "var(--color-success)",  border: "1px solid #86efac" },
  warning: { background: "var(--color-warning-light)", color: "var(--color-warning)",  border: "1px solid #fcd34d" },
  info:    { background: "var(--color-primary-light)",  color: "var(--color-primary)", border: "1px solid #5eead4" },
};

interface StatusMessageProps {
  variant: StatusVariant;
  children: React.ReactNode;
}

export function StatusMessage({ variant, children }: StatusMessageProps) {
  return (
    <div
      role={variant === "error" ? "alert" : "status"}
      style={{
        ...statusStyles[variant],
        borderRadius: "var(--radius-md)",
        padding: "10px 14px",
        fontSize: 14,
        fontWeight: 500,
      }}
    >
      {children}
    </div>
  );
}

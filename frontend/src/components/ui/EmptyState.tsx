import React from "react";

interface EmptyStateProps {
  message: string;
  action?: React.ReactNode;
}

export function EmptyState({ message, action }: EmptyStateProps) {
  return (
    <div
      style={{
        padding: "40px 20px",
        textAlign: "center",
        color: "var(--color-text-muted)",
      }}
    >
      <p style={{ margin: "0 0 16px", fontSize: 15 }}>{message}</p>
      {action}
    </div>
  );
}

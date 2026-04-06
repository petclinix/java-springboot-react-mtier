import React from "react";

type StatusVariant = "error" | "success" | "warning" | "info";

const statusClass: Record<StatusVariant, string> = {
  error:   "bg-danger-light text-danger border border-[#fca5a5]",
  success: "bg-success-light text-success border border-[#86efac]",
  warning: "bg-warning-light text-warning border border-[#fcd34d]",
  info:    "bg-primary-light text-primary border border-[#5eead4]",
};

interface StatusMessageProps {
  variant: StatusVariant;
  children: React.ReactNode;
}

export function StatusMessage({ variant, children }: StatusMessageProps) {
  return (
    <div
      role={variant === "error" ? "alert" : "status"}
      className={[
        statusClass[variant],
        "rounded-card px-[14px] py-[10px] text-[14px] font-medium",
      ].join(" ")}
    >
      {children}
    </div>
  );
}

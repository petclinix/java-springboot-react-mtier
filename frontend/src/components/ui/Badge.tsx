import React from "react";

type BadgeVariant = "owner" | "vet" | "admin" | "active" | "inactive" | "neutral";

const badgeClass: Record<BadgeVariant, string> = {
  owner:    "[background:var(--color-role-owner)] [color:var(--color-role-owner-text)]",
  vet:      "[background:var(--color-role-vet)] [color:var(--color-role-vet-text)]",
  admin:    "[background:var(--color-role-admin)] [color:var(--color-role-admin-text)]",
  active:   "bg-success-light text-success",
  inactive: "bg-danger-light text-danger",
  neutral:  "bg-surface-hover text-muted",
};

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
}

export function Badge({ variant = "neutral", children }: BadgeProps) {
  return (
    <span
      className={[
        badgeClass[variant],
        "rounded-badge px-[8px] py-[2px] text-[12px] font-semibold tracking-[0.03em] inline-block",
      ].join(" ")}
    >
      {children}
    </span>
  );
}

import React from "react";

type BadgeVariant = "owner" | "vet" | "admin" | "active" | "inactive" | "neutral";

const badgeStyles: Record<BadgeVariant, React.CSSProperties> = {
  owner:    { background: "var(--color-role-owner)",   color: "var(--color-role-owner-text)" },
  vet:      { background: "var(--color-role-vet)",     color: "var(--color-role-vet-text)" },
  admin:    { background: "var(--color-role-admin)",   color: "var(--color-role-admin-text)" },
  active:   { background: "var(--color-success-light)",color: "var(--color-success)" },
  inactive: { background: "var(--color-danger-light)", color: "var(--color-danger)" },
  neutral:  { background: "var(--color-surface-hover)",color: "var(--color-text-muted)" },
};

interface BadgeProps {
  variant?: BadgeVariant;
  children: React.ReactNode;
}

export function Badge({ variant = "neutral", children }: BadgeProps) {
  return (
    <span
      style={{
        ...badgeStyles[variant],
        borderRadius: "var(--radius-sm)",
        padding: "2px 8px",
        fontSize: 12,
        fontWeight: 600,
        letterSpacing: "0.03em",
        display: "inline-block",
      }}
    >
      {children}
    </span>
  );
}

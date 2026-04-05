import React from "react";

type Variant = "primary" | "secondary" | "danger" | "ghost";
type Size = "sm" | "md";

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const variantStyles: Record<Variant, React.CSSProperties> = {
  primary: {
    background: "var(--color-primary)",
    color: "#fff",
    border: "1px solid transparent",
  },
  secondary: {
    background: "var(--color-surface)",
    color: "var(--color-text)",
    border: "1px solid var(--color-border-strong)",
  },
  danger: {
    background: "var(--color-danger)",
    color: "#fff",
    border: "1px solid transparent",
  },
  ghost: {
    background: "transparent",
    color: "var(--color-primary)",
    border: "1px solid transparent",
    textDecoration: "underline",
  },
};

const sizeStyles: Record<Size, React.CSSProperties> = {
  sm: { padding: "4px 10px", fontSize: 13 },
  md: { padding: "8px 16px", fontSize: 14 },
};

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  children,
  disabled,
  style,
  ...props
}: ButtonProps) {
  return (
    <button
      disabled={disabled || loading}
      style={{
        ...variantStyles[variant],
        ...sizeStyles[size],
        borderRadius: "var(--radius-md)",
        fontWeight: 500,
        cursor: disabled || loading ? "not-allowed" : "pointer",
        opacity: disabled || loading ? 0.65 : 1,
        display: "inline-flex",
        alignItems: "center",
        gap: 6,
        transition: "opacity 0.15s",
        fontFamily: "inherit",
        ...style,
      }}
      {...props}
    >
      {loading ? "Loading…" : children}
    </button>
  );
}

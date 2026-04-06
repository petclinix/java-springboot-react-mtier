import React from "react";

type Variant = "primary" | "secondary" | "danger" | "ghost";
type Size = "sm" | "md";

interface ButtonProps extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  loading?: boolean;
}

const variantClass: Record<Variant, string> = {
  primary:   "bg-primary text-white border border-transparent",
  secondary: "bg-surface text-[#1e293b] border border-strong",
  danger:    "bg-danger text-white border border-transparent",
  ghost:     "bg-transparent text-primary border border-transparent underline",
};

const sizeClass: Record<Size, string> = {
  sm: "px-[10px] py-[4px] text-[13px]",
  md: "px-[16px] py-[8px] text-[14px]",
};

export function Button({
  variant = "primary",
  size = "md",
  loading = false,
  children,
  disabled,
  className = "",
  ...props
}: ButtonProps) {
  const isDisabled = disabled || loading;
  return (
    <button
      disabled={isDisabled}
      className={[
        variantClass[variant],
        sizeClass[size],
        "rounded-card font-medium inline-flex items-center gap-[6px] transition-opacity duration-150 font-[inherit]",
        isDisabled ? "cursor-not-allowed opacity-[0.65]" : "cursor-pointer",
        className,
      ].join(" ")}
      {...props}
    >
      {loading ? "Loading…" : children}
    </button>
  );
}

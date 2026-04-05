import React from "react";

type InputProps = React.InputHTMLAttributes<HTMLInputElement>;

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  function Input({ style, ...props }, ref) {
    return (
      <input
        ref={ref}
        style={{
          width: "100%",
          padding: "8px 12px",
          fontSize: 14,
          border: "1px solid var(--color-border-strong)",
          borderRadius: "var(--radius-md)",
          background: "var(--color-surface)",
          color: "var(--color-text)",
          outline: "none",
          fontFamily: "inherit",
          ...style,
        }}
        {...props}
      />
    );
  }
);

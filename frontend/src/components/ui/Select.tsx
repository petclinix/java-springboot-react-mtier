import React from "react";

type SelectProps = React.SelectHTMLAttributes<HTMLSelectElement>;

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  function Select({ style, children, ...props }, ref) {
    return (
      <select
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
      >
        {children}
      </select>
    );
  }
);

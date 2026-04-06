import React from "react";

type SelectProps = React.SelectHTMLAttributes<HTMLSelectElement>;

export const Select = React.forwardRef<HTMLSelectElement, SelectProps>(
  function Select({ className = "", children, ...props }, ref) {
    return (
      <select
        ref={ref}
        className={[
          "w-full px-[12px] py-[8px] text-[14px]",
          "border border-strong rounded-card",
          "bg-surface text-[#1e293b]",
          "outline-none font-[inherit]",
          className,
        ].join(" ")}
        {...props}
      >
        {children}
      </select>
    );
  }
);

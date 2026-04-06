import React from "react";

type InputProps = React.InputHTMLAttributes<HTMLInputElement>;

export const Input = React.forwardRef<HTMLInputElement, InputProps>(
  function Input({ className = "", ...props }, ref) {
    return (
      <input
        ref={ref}
        className={[
          "w-full px-[12px] py-[8px] text-[14px]",
          "border border-strong rounded-card",
          "bg-surface text-[#1e293b]",
          "outline-none font-[inherit]",
          className,
        ].join(" ")}
        {...props}
      />
    );
  }
);

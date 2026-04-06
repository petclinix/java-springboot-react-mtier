import React from "react";

interface FormFieldProps {
  label: string;
  children: React.ReactNode;
  error?: string;
  hint?: string;
}

export function FormField({ label, children, error, hint }: FormFieldProps) {
  return (
    <div className="flex flex-col gap-[4px]">
      <label className="text-[13px] font-semibold text-[#1e293b]">
        {label}
      </label>
      {children}
      {hint && <p className="m-0 text-[12px] text-muted">{hint}</p>}
      {error && <p className="m-0 text-[12px] text-danger">{error}</p>}
    </div>
  );
}

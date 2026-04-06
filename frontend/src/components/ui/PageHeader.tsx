import React from "react";

interface PageHeaderProps {
  title: string;
  actions?: React.ReactNode;
}

export function PageHeader({ title, actions }: PageHeaderProps) {
  return (
    <div className="flex justify-between items-center mb-[24px]">
      <h1 className="m-0 text-[24px] font-bold text-[#1e293b]">
        {title}
      </h1>
      {actions && <div className="flex gap-[8px]">{actions}</div>}
    </div>
  );
}

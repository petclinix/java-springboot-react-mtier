import React from "react";

interface PageLayoutProps {
  children: React.ReactNode;
  narrow?: boolean;  // 640px for forms; default 960px for data pages
}

export function PageLayout({ children, narrow = false }: PageLayoutProps) {
  return (
    <div
      className={[
        "mx-auto px-[20px] py-[32px]",
        narrow ? "max-w-[640px]" : "max-w-[960px]",
      ].join(" ")}
    >
      {children}
    </div>
  );
}

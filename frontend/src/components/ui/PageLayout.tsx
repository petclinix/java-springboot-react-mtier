import React from "react";

interface PageLayoutProps {
  children: React.ReactNode;
  narrow?: boolean;  // 640px for forms; default 960px for data pages
}

export function PageLayout({ children, narrow = false }: PageLayoutProps) {
  return (
    <div
      style={{
        maxWidth: narrow ? 640 : 960,
        margin: "0 auto",
        padding: "32px 20px",
      }}
    >
      {children}
    </div>
  );
}

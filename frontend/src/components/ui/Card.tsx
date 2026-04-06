import React from "react";

interface CardProps {
  children: React.ReactNode;
  className?: string;
}

export function Card({ children, className = "" }: CardProps) {
  return (
    <div
      className={[
        "bg-surface border border-border rounded-panel shadow-card px-[24px] py-[20px]",
        className,
      ].join(" ")}
    >
      {children}
    </div>
  );
}

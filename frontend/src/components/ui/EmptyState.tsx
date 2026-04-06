import React from "react";

interface EmptyStateProps {
  message: string;
  action?: React.ReactNode;
}

export function EmptyState({ message, action }: EmptyStateProps) {
  return (
    <div className="px-[20px] py-[40px] text-center text-muted">
      <p className="m-0 mb-[16px] text-[15px]">{message}</p>
      {action}
    </div>
  );
}

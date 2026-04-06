import React from "react";

interface Column<T> {
  header: string;
  render: (row: T) => React.ReactNode;
  width?: string;
}

interface DataTableProps<T> {
  columns: Column<T>[];
  rows: T[];
  keyFn: (row: T) => string | number;
  emptyMessage?: string;
}

export function DataTable<T>({ columns, rows, keyFn, emptyMessage = "No data." }: DataTableProps<T>) {
  return (
    <table className="w-full border-collapse">
      <thead>
        <tr className="border-b-[2px] border-border">
          {columns.map((col, i) => (
            <th
              key={i}
              style={{ width: col.width }}
              className="text-left px-[12px] py-[10px] text-[12px] font-bold text-muted tracking-[0.05em] uppercase"
            >
              {col.header}
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.length === 0 ? (
          <tr>
            <td
              colSpan={columns.length}
              className="px-[12px] py-[32px] text-center text-muted text-[14px]"
            >
              {emptyMessage}
            </td>
          </tr>
        ) : (
          rows.map((row) => (
            <tr
              key={keyFn(row)}
              className="border-b border-border transition-colors duration-100 hover:bg-surface-hover"
            >
              {columns.map((col, i) => (
                <td key={i} className="px-[12px] py-[10px] text-[14px]">
                  {col.render(row)}
                </td>
              ))}
            </tr>
          ))
        )}
      </tbody>
    </table>
  );
}

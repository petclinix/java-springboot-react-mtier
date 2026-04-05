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
    <table style={{ width: "100%", borderCollapse: "collapse" }}>
      <thead>
        <tr style={{ borderBottom: "2px solid var(--color-border)" }}>
          {columns.map((col, i) => (
            <th
              key={i}
              style={{
                textAlign: "left",
                padding: "10px 12px",
                fontSize: 12,
                fontWeight: 700,
                color: "var(--color-text-muted)",
                letterSpacing: "0.05em",
                textTransform: "uppercase",
                width: col.width,
              }}
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
              style={{
                padding: "32px 12px",
                textAlign: "center",
                color: "var(--color-text-muted)",
                fontSize: 14,
              }}
            >
              {emptyMessage}
            </td>
          </tr>
        ) : (
          rows.map((row) => (
            <tr
              key={keyFn(row)}
              style={{
                borderBottom: "1px solid var(--color-border)",
                transition: "background 0.1s",
              }}
              onMouseEnter={(e) =>
                ((e.currentTarget as HTMLElement).style.background = "var(--color-surface-hover)")
              }
              onMouseLeave={(e) =>
                ((e.currentTarget as HTMLElement).style.background = "")
              }
            >
              {columns.map((col, i) => (
                <td key={i} style={{ padding: "10px 12px", fontSize: 14 }}>
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

import * as React from "react";
import { Link, useNavigate } from "react-router-dom";
import {
  closestCenter,
  DndContext,
  KeyboardSensor,
  MouseSensor,
  TouchSensor,
  useSensor,
  useSensors,
  type DragEndEvent,
  type UniqueIdentifier,
} from "@dnd-kit/core";
import { restrictToHorizontalAxis, restrictToVerticalAxis } from "@dnd-kit/modifiers";
import {
  SortableContext,
  arrayMove,
  useSortable,
  verticalListSortingStrategy,
} from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import {
  flexRender,
  getCoreRowModel,
  getFilteredRowModel,
  getPaginationRowModel,
  getSortedRowModel,
  useReactTable,
  type ColumnDef,
  type ColumnFiltersState,
  type SortingState,
  type VisibilityState,
  type Row,
} from "@tanstack/react-table";
import { ChevronDown, ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight, Eye, FileText, GripVertical, MoreHorizontal, Search, Trash2 } from "lucide-react";
import { deleteMP4History, reorderMP4History } from "@/services/core/mp4-history";
import type { MP4HistoryListItem } from "@/lib/history-types";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuCheckboxItem,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { notification } from "@/components/ui/notification";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";

function formatDate(value?: string) {
  if (!value) return "--";
  return new Date(value).toLocaleString();
}

function DragHandle({ id }: { id: string }) {
  const { attributes, listeners } = useSortable({ id });
  return (
    <Button
      {...attributes}
      {...listeners}
      variant="ghost"
      size="icon"
      className="size-8 rounded-md text-muted-foreground"
    >
      <GripVertical className="size-4" />
      <span className="sr-only">Drag to reorder</span>
    </Button>
  );
}

function columnClass(columnId: string) {
  const classes: Record<string, string> = {
    drag: "w-[48px]",
    title: "w-[360px]",
    status: "w-[120px]",
    task_id: "w-[240px]",
    data_file: "w-[320px]",
    created_at: "w-[210px]",
    actions: "w-[64px]",
  };
  return classes[columnId] || "";
}

function getColumns(onDelete: (item: MP4HistoryListItem) => void): ColumnDef<MP4HistoryListItem>[] {
  return [
  {
    id: "drag",
    header: () => null,
    cell: ({ row }) => <DragHandle id={row.original.id} />,
  },
  {
    accessorKey: "title",
    header: "Title",
    filterFn: (row, _columnId, value) => {
      const keyword = String(value || "").trim().toLowerCase();
      if (!keyword) return true;
      const item = row.original;
      return [item.title, item.file_name || "", item.task_id, item.status, item.data_file]
        .join(" ")
        .toLowerCase()
        .includes(keyword);
    },
    cell: ({ row }) => (
      <div className="min-w-0 space-y-1">
        <Link
          to={`/mp4-to-word/history/${row.original.id}`}
          className="block truncate text-sm font-medium text-foreground hover:underline"
          title={row.original.title}
        >
          {row.original.title}
        </Link>
        <div className="truncate text-xs text-muted-foreground">{row.original.file_name || "--"}</div>
      </div>
    ),
  },
  {
    accessorKey: "status",
    header: "Status",
    cell: ({ row }) => <Badge variant="secondary">{row.original.status || "unknown"}</Badge>,
  },
  {
    accessorKey: "task_id",
    header: "Task",
    cell: ({ row }) => <span className="block max-w-[180px] truncate font-mono text-xs text-muted-foreground">{row.original.task_id}</span>,
  },
  {
    accessorKey: "data_file",
    header: "Data File",
    cell: ({ row }) => <span className="block max-w-[240px] truncate text-xs text-muted-foreground">{row.original.data_file}</span>,
  },
  {
    accessorKey: "created_at",
    header: "Created",
    cell: ({ row }) => <span className="whitespace-nowrap text-sm text-muted-foreground">{formatDate(row.original.created_at)}</span>,
  },
  {
    id: "actions",
    header: () => null,
    cell: ({ row }) => (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon" className="size-8 rounded-md text-muted-foreground">
            <MoreHorizontal className="size-4" />
            <span className="sr-only">Open menu</span>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-40">
          <DropdownMenuItem asChild>
            <Link to={`/mp4-to-word/history/${row.original.id}`}>
              <Eye className="size-4" />
              View detail
            </Link>
          </DropdownMenuItem>
          <DropdownMenuItem
            className="text-red-600 focus:text-red-600"
            onClick={() => onDelete(row.original)}
          >
            <Trash2 className="size-4" />
            Delete
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    ),
  },
  ];
}

function DraggableRow({ row }: { row: Row<MP4HistoryListItem> }) {
  const { transform, transition, setNodeRef, isDragging } = useSortable({ id: row.original.id });

  return (
    <TableRow
      ref={setNodeRef}
      data-dragging={isDragging}
      className="border-b bg-background transition hover:bg-muted/40 data-[dragging=true]:opacity-70"
      style={{ transform: CSS.Transform.toString(transform), transition }}
    >
      {row.getVisibleCells().map((cell) => (
        <TableCell key={cell.id} className={columnClass(cell.column.id)}>
          {flexRender(cell.column.columnDef.cell, cell.getContext())}
        </TableCell>
      ))}
    </TableRow>
  );
}

export function MP4HistoryDataTable({ data: initialData }: { data: MP4HistoryListItem[] }) {
  const navigate = useNavigate();
  const [data, setData] = React.useState(() => initialData);
  const [sorting, setSorting] = React.useState<SortingState>([{ id: "created_at", desc: true }]);
  const [columnFilters, setColumnFilters] = React.useState<ColumnFiltersState>([]);
  const [columnVisibility, setColumnVisibility] = React.useState<VisibilityState>({});
  const [pagination, setPagination] = React.useState({ pageIndex: 0, pageSize: 10 });

  React.useEffect(() => {
    setData(initialData);
  }, [initialData]);

  const dataIds = React.useMemo<UniqueIdentifier[]>(() => data.map((item) => item.id), [data]);
  const sensors = useSensors(useSensor(MouseSensor), useSensor(TouchSensor), useSensor(KeyboardSensor));
  const doneCount = data.filter((item) => (item.status || "").toLowerCase() === "done").length;
  const todayCount = data.filter((item) => {
    if (!item.created_at) return false;
    return new Date(item.created_at).toDateString() === new Date().toDateString();
  }).length;

  async function handleDelete(item: MP4HistoryListItem) {
    try {
      const deleted = await deleteMP4History(item.id);
      setData((prev) => prev.filter((entry) => entry.id !== item.id));
      notification.success({
        title: "已删除历史记录",
        description: deleted.title || item.title,
      });
      navigate("/mp4-to-word/history", { replace: true });
    } catch (error) {
      const msg = error instanceof Error ? error.message : "删除历史记录失败";
      notification.danger({ title: "删除历史记录失败", description: msg });
    }
  }

  const columns = React.useMemo(() => getColumns(handleDelete), []);

  const table = useReactTable({
    data,
    columns,
    state: {
      sorting,
      columnFilters,
      columnVisibility,
      pagination,
    },
    getRowId: (row) => row.id,
    onSortingChange: setSorting,
    onColumnFiltersChange: setColumnFilters,
    onColumnVisibilityChange: setColumnVisibility,
    onPaginationChange: setPagination,
    getCoreRowModel: getCoreRowModel(),
    getFilteredRowModel: getFilteredRowModel(),
    getPaginationRowModel: getPaginationRowModel(),
    getSortedRowModel: getSortedRowModel(),
  });
  const filteredCount = table.getFilteredRowModel().rows.length;

  async function handleDragEnd(event: DragEndEvent) {
    const { active, over } = event;
    if (!active || !over || active.id === over.id) return;

    const reordered = arrayMove(data, dataIds.indexOf(active.id), dataIds.indexOf(over.id));
    setData(reordered);
    try {
      const saved = await reorderMP4History(reordered.map((item) => item.id));
      setData(saved);
    } catch (error) {
      setData(data);
      const msg = error instanceof Error ? error.message : "保存历史顺序失败";
      notification.danger({ title: "保存历史顺序失败", description: msg });
    }
  }

  return (
    <div className="space-y-4">
      <div className="grid gap-3 sm:grid-cols-3">
        {[
          { label: "Records", value: data.length, hint: `${filteredCount} visible` },
          { label: "Done", value: doneCount, hint: "completed workflows" },
          { label: "Today", value: todayCount, hint: "created today" },
        ].map((item) => (
          <div key={item.label} className="rounded-md bg-muted/60 px-4 py-3">
            <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
              <FileText className="size-4" />
              {item.label}
            </div>
            <div className="mt-2 font-mono text-2xl font-semibold text-foreground">{item.value}</div>
            <div className="mt-1 text-xs text-muted-foreground">{item.hint}</div>
          </div>
        ))}
      </div>

      <div className="flex flex-col gap-3 xl:flex-row xl:items-center xl:justify-between">
        <div className="relative w-full xl:max-w-sm">
          <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            placeholder="搜索标题、文件名、任务 ID"
            value={(table.getColumn("title")?.getFilterValue() as string) ?? ""}
            onChange={(event) => table.getColumn("title")?.setFilterValue(event.target.value)}
            className="pl-9"
          />
        </div>
        <div className="flex items-center justify-between gap-3">
          <span className="text-sm text-muted-foreground">
            共 {filteredCount} / {data.length} 条
          </span>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="outline" size="sm" className="rounded-md bg-background">
                Columns
                <ChevronDown className="size-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end" className="w-48">
              {table
                .getAllColumns()
                .filter((column) => column.getCanHide())
                .map((column) => (
                  <DropdownMenuCheckboxItem
                    key={column.id}
                    className="capitalize"
                    checked={column.getIsVisible()}
                    onCheckedChange={(value) => column.toggleVisibility(!!value)}
                  >
                    {column.id.replace("_", " ")}
                  </DropdownMenuCheckboxItem>
                ))}
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </div>

      <div className="max-w-full overflow-hidden rounded-md bg-muted/50 [&_[data-slot=table-container]]:max-h-[66vh] [&_[data-slot=table-container]]:overflow-auto [&_[data-slot=table-container]]:[scrollbar-width:thin]">
        <DndContext
          collisionDetection={closestCenter}
          modifiers={[restrictToVerticalAxis, restrictToHorizontalAxis]}
          onDragEnd={handleDragEnd}
          sensors={sensors}
        >
          <Table className="min-w-[1360px] table-fixed">
            <TableHeader className="[&_th]:sticky [&_th]:top-0 [&_th]:z-20 [&_th]:bg-muted">
              {table.getHeaderGroups().map((headerGroup) => (
                <TableRow key={headerGroup.id} className="border-b hover:bg-transparent">
                  {headerGroup.headers.map((header) => (
                    <TableHead key={header.id} className={`h-10 text-xs font-semibold uppercase text-muted-foreground ${columnClass(header.column.id)}`}>
                      {header.isPlaceholder ? null : flexRender(header.column.columnDef.header, header.getContext())}
                    </TableHead>
                  ))}
                </TableRow>
              ))}
            </TableHeader>
            <TableBody>
              {table.getRowModel().rows?.length ? (
                <SortableContext items={dataIds} strategy={verticalListSortingStrategy}>
                  {table.getRowModel().rows.map((row) => (
                    <DraggableRow key={row.id} row={row} />
                  ))}
                </SortableContext>
              ) : (
                <TableRow>
                  <TableCell colSpan={columns.length} className="h-24 text-center text-muted-foreground">
                    No history records found.
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </DndContext>
      </div>

      <div className="flex flex-col items-stretch justify-between gap-3 rounded-md bg-muted/50 px-3 py-3 sm:flex-row sm:items-center">
        <div className="hidden text-sm text-muted-foreground sm:block">
          {table.getFilteredRowModel().rows.length} record(s)
        </div>
        <div className="flex items-center justify-between gap-3 sm:justify-start sm:gap-6">
          <div className="hidden items-center gap-2 lg:flex">
            <Label htmlFor="history-rows-per-page" className="text-sm font-medium">
              Rows
            </Label>
            <Select value={`${table.getState().pagination.pageSize}`} onValueChange={(value) => table.setPageSize(Number(value))}>
              <SelectTrigger id="history-rows-per-page" size="sm" className="w-20 rounded-md bg-background">
                <SelectValue />
              </SelectTrigger>
              <SelectContent side="top">
                {[10, 20, 30, 40].map((pageSize) => (
                  <SelectItem key={pageSize} value={`${pageSize}`}>
                    {pageSize}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
          <div className="whitespace-nowrap text-sm font-medium">
            Page {table.getState().pagination.pageIndex + 1} of {table.getPageCount() || 1}
          </div>
          <div className="flex shrink-0 items-center gap-2">
            <Button variant="outline" size="icon" className="hidden size-8 rounded-md bg-background lg:flex" onClick={() => table.setPageIndex(0)} disabled={!table.getCanPreviousPage()}>
              <ChevronsLeft className="size-4" />
            </Button>
            <Button variant="outline" size="icon" className="size-8 rounded-md bg-background" onClick={() => table.previousPage()} disabled={!table.getCanPreviousPage()}>
              <ChevronLeft className="size-4" />
            </Button>
            <Button variant="outline" size="icon" className="size-8 rounded-md bg-background" onClick={() => table.nextPage()} disabled={!table.getCanNextPage()}>
              <ChevronRight className="size-4" />
            </Button>
            <Button variant="outline" size="icon" className="hidden size-8 rounded-md bg-background lg:flex" onClick={() => table.setPageIndex(table.getPageCount() - 1)} disabled={!table.getCanNextPage()}>
              <ChevronsRight className="size-4" />
            </Button>
          </div>
        </div>
      </div>
    </div>
  );
}

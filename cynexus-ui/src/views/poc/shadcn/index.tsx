import { useState, type ReactNode } from "react"
import {
  Area,
  AreaChart,
  CartesianGrid,
  Line,
  LineChart,
  Legend,
  Tooltip as RechartsTooltip,
  XAxis,
  YAxis,
} from "recharts"
import {
  Bell,
  CalendarDays,
  Check,
  CheckCircle2,
  ChevronRight,
  CircleHelp,
  Command as CommandIcon,
  Download,
  FileText,
  Filter,
  Image as ImageIcon,
  Inbox,
  MoreHorizontal,
  PanelLeft,
  Plus,
  Search,
  Settings2,
  ShieldCheck,
  Sparkles,
  Terminal,
  Trash2,
  Upload,
  UserRound,
  Zap,
} from "lucide-react"

import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from "@/components/ui/accordion"
import { AlertDialog, AlertDialogAction, AlertDialogCancel, AlertDialogContent, AlertDialogDescription, AlertDialogFooter, AlertDialogHeader, AlertDialogMedia, AlertDialogTitle, AlertDialogTrigger } from "@/components/ui/alert-dialog"
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert"
import { AspectRatio } from "@/components/ui/aspect-ratio"
import { Attachment, AttachmentAction, AttachmentActions, AttachmentContent, AttachmentDescription, AttachmentGroup, AttachmentMedia, AttachmentTitle } from "@/components/ui/attachment"
import { Avatar, AvatarFallback, AvatarImage, AvatarBadge, AvatarGroup, AvatarGroupCount } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"
import { Breadcrumb, BreadcrumbEllipsis, BreadcrumbItem, BreadcrumbLink, BreadcrumbList, BreadcrumbPage, BreadcrumbSeparator } from "@/components/ui/breadcrumb"
import { Bubble, BubbleContent, BubbleGroup, BubbleReactions } from "@/components/ui/bubble"
import { ButtonGroup, ButtonGroupSeparator, ButtonGroupText } from "@/components/ui/button-group"
import { Button } from "@/components/ui/button"
import { Calendar } from "@/components/ui/calendar"
import { Card, CardAction, CardContent, CardDescription, CardFooter, CardHeader, CardTitle } from "@/components/ui/card"
import { Carousel, CarouselContent, CarouselItem, CarouselNext, CarouselPrevious } from "@/components/ui/carousel"
import { ChartContainer, ChartLegendContent, ChartTooltipContent, type ChartConfig } from "@/components/ui/chart"
import { Checkbox } from "@/components/ui/checkbox"
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from "@/components/ui/collapsible"
import { Combobox, ComboboxContent, ComboboxEmpty, ComboboxInput, ComboboxItem, ComboboxList } from "@/components/ui/combobox"
import { Command, CommandEmpty, CommandGroup, CommandInput, CommandItem, CommandList, CommandSeparator, CommandShortcut } from "@/components/ui/command"
import { ConfirmDialog } from "@/components/ui/confirm-dialog"
import { ContextMenu, ContextMenuCheckboxItem, ContextMenuContent, ContextMenuItem, ContextMenuLabel, ContextMenuRadioGroup, ContextMenuRadioItem, ContextMenuSeparator, ContextMenuShortcut, ContextMenuSub, ContextMenuSubContent, ContextMenuSubTrigger, ContextMenuTrigger } from "@/components/ui/context-menu"
import { CountUp } from "@/components/ui/count-up"
import { DatePicker } from "@/components/ui/date-picker"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog"
import { DirectionProvider } from "@/components/ui/direction"
import { Drawer, DrawerContent, DrawerDescription, DrawerFooter, DrawerHeader, DrawerTitle, DrawerTrigger } from "@/components/ui/drawer"
import { DropdownMenu, DropdownMenuCheckboxItem, DropdownMenuContent, DropdownMenuGroup, DropdownMenuItem, DropdownMenuLabel, DropdownMenuRadioGroup, DropdownMenuRadioItem, DropdownMenuSeparator, DropdownMenuShortcut, DropdownMenuSub, DropdownMenuSubContent, DropdownMenuSubTrigger, DropdownMenuTrigger } from "@/components/ui/dropdown-menu"
import { Empty, EmptyContent, EmptyDescription, EmptyHeader, EmptyMedia, EmptyTitle } from "@/components/ui/empty"
import { Field, FieldContent, FieldDescription, FieldGroup, FieldLabel, FieldLegend, FieldSeparator, FieldSet } from "@/components/ui/field"
import { HoverCard, HoverCardContent, HoverCardTrigger } from "@/components/ui/hover-card"
import { InputGroup, InputGroupAddon, InputGroupButton, InputGroupInput, InputGroupTextarea } from "@/components/ui/input-group"
import { InputOTP, InputOTPGroup, InputOTPSeparator, InputOTPSlot } from "@/components/ui/input-otp"
import { Input } from "@/components/ui/input"
import { Item, ItemActions, ItemContent, ItemDescription, ItemGroup, ItemMedia, ItemSeparator, ItemTitle } from "@/components/ui/item"
import { Kbd, KbdGroup } from "@/components/ui/kbd"
import { Label } from "@/components/ui/label"
import { Marker, MarkerContent, MarkerIcon } from "@/components/ui/marker"
import { Menubar, MenubarCheckboxItem, MenubarContent, MenubarItem, MenubarLabel, MenubarMenu, MenubarRadioGroup, MenubarRadioItem, MenubarSeparator, MenubarShortcut, MenubarSub, MenubarSubContent, MenubarSubTrigger, MenubarTrigger } from "@/components/ui/menubar"
import { Message, MessageAvatar, MessageContent, MessageFooter, MessageGroup, MessageHeader } from "@/components/ui/message"
import { MessageScroller, MessageScrollerButton, MessageScrollerContent, MessageScrollerItem, MessageScrollerProvider, MessageScrollerViewport } from "@/components/ui/message-scroller"
import { NativeSelect, NativeSelectOptGroup, NativeSelectOption } from "@/components/ui/native-select"
import { NavigationMenu, NavigationMenuContent, NavigationMenuItem, NavigationMenuLink, NavigationMenuList, NavigationMenuTrigger } from "@/components/ui/navigation-menu"
import { notification } from "@/components/ui/notification"
import { Pagination, PaginationContent, PaginationEllipsis, PaginationItem, PaginationLink, PaginationNext, PaginationPrevious } from "@/components/ui/pagination"
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover"
import { Progress } from "@/components/ui/progress"
import { RadioGroup, RadioGroupItem } from "@/components/ui/radio-group"
import { ResizableHandle, ResizablePanel, ResizablePanelGroup } from "@/components/ui/resizable"
import { ScrollArea } from "@/components/ui/scroll-area"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Separator } from "@/components/ui/separator"
import { Sheet, SheetContent, SheetDescription, SheetFooter, SheetHeader, SheetTitle, SheetTrigger } from "@/components/ui/sheet"
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarGroup,
  SidebarGroupAction,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarInput,
  SidebarMenu,
  SidebarMenuAction,
  SidebarMenuBadge,
  SidebarMenuButton,
  SidebarMenuItem,
  SidebarMenuSkeleton,
  SidebarMenuSub,
  SidebarMenuSubButton,
  SidebarMenuSubItem,
  SidebarProvider,
  SidebarSeparator,
} from "@/components/ui/sidebar"
import { Skeleton } from "@/components/ui/skeleton"
import { Spinner } from "@/components/ui/spinner"
import { Slider } from "@/components/ui/slider"
import { Table, TableBody, TableCaption, TableCell, TableFooter, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Textarea } from "@/components/ui/textarea"
import { ToggleGroup, ToggleGroupItem } from "@/components/ui/toggle-group"
import { Toggle } from "@/components/ui/toggle"
import { Tooltip, TooltipContent, TooltipProvider, TooltipTrigger } from "@/components/ui/tooltip"
import { Switch } from "@/components/ui/switch"
import { WorkspaceShell } from "@/layout/workspace-shell"

const chartConfig = {
  desktop: { label: "Desktop", color: "#343434" },
  mobile: { label: "Mobile", color: "#0071e3" },
} satisfies ChartConfig

const chartData = [
  { month: "Jan", desktop: 186, mobile: 80 },
  { month: "Feb", desktop: 305, mobile: 200 },
  { month: "Mar", desktop: 237, mobile: 120 },
  { month: "Apr", desktop: 273, mobile: 190 },
  { month: "May", desktop: 209, mobile: 130 },
  { month: "Jun", desktop: 214, mobile: 140 },
]

function ShowcaseSection({ title, description, children }: { title: string; description: string; children: ReactNode }) {
  return (
    <section className="overflow-hidden rounded-lg border border-border/70 bg-card">
      <div className="flex flex-col gap-1 border-b border-border/60 bg-muted/20 px-4 py-3 sm:flex-row sm:items-baseline sm:justify-between">
        <h2 className="text-sm font-semibold text-foreground">{title}</h2>
        <p className="text-xs text-muted-foreground">{description}</p>
      </div>
      <div className="grid gap-4 p-4 md:grid-cols-2 xl:grid-cols-3">{children}</div>
    </section>
  )
}

function Example({ label, children, className = "" }: { label: string; children: ReactNode; className?: string }) {
  return (
    <div className={`min-w-0 space-y-2 ${className}`}>
      <div className="text-[11px] font-medium uppercase tracking-[0.08em] text-muted-foreground">{label}</div>
      <div className="min-w-0">{children}</div>
    </div>
  )
}

function ButtonAndStatusSection() {
  const [progress, setProgress] = useState(68)
  const [toggle, setToggle] = useState(false)
  const [groupValue, setGroupValue] = useState("overview")

  return (
    <ShowcaseSection title="Actions & status" description="Buttons, badges, alerts, progress, loading, and selection states">
      <Example label="Button variants" className="xl:col-span-2">
        <div className="flex flex-wrap gap-2">
          <Button><Plus />Create</Button>
          <Button variant="secondary"><Settings2 />Tune</Button>
          <Button variant="outline"><Filter />Filter</Button>
          <Button variant="ghost" size="icon" aria-label="More actions" title="More actions"><MoreHorizontal /></Button>
          <Button variant="destructive"><Trash2 />Remove</Button>
          <Button variant="link">View details</Button>
        </div>
      </Example>
      <Example label="Badge and marker">
        <div className="space-y-3">
          <div className="flex flex-wrap gap-2"><Badge>Ready</Badge><Badge variant="secondary">Draft</Badge><Badge variant="outline">Snapshot</Badge><Badge variant="destructive">Needs review</Badge></div>
          <Marker variant="separator"><MarkerIcon><Zap /></MarkerIcon><MarkerContent>Semantic status marker</MarkerContent></Marker>
        </div>
      </Example>
      <Example label="Alert">
        <Alert><ShieldCheck /><AlertTitle>Style checkpoint</AlertTitle><AlertDescription>All examples use the shared semantic tokens.</AlertDescription></Alert>
      </Example>
      <Example label="Progress and spinner">
        <div className="space-y-3"><Progress value={progress} /><div className="flex items-center justify-between text-xs text-muted-foreground"><span>{progress}% complete</span><Spinner /></div><Button variant="outline" size="sm" onClick={() => setProgress((value) => value >= 100 ? 20 : value + 20)}>Advance</Button></div>
      </Example>
      <Example label="Toggle and group">
        <div className="space-y-3"><Toggle pressed={toggle} onPressedChange={setToggle} aria-label="Pin selection"><Sparkles />{toggle ? "Pinned" : "Pin"}</Toggle><ToggleGroup type="single" value={groupValue} onValueChange={(value) => value && setGroupValue(value)}><ToggleGroupItem value="overview">Overview</ToggleGroupItem><ToggleGroupItem value="tokens">Tokens</ToggleGroupItem><ToggleGroupItem value="states">States</ToggleGroupItem></ToggleGroup></div>
      </Example>
    </ShowcaseSection>
  )
}

function FormSection() {
  const [checked, setChecked] = useState(true)
  const [selectValue, setSelectValue] = useState("neutral")
  const [date, setDate] = useState<Date | null>(new Date(2026, 0, 15))
  const [calendarDate, setCalendarDate] = useState<Date | undefined>(new Date(2026, 0, 15))

  return (
    <ShowcaseSection title="Forms & inputs" description="Fields, labels, select, checkbox, calendar, date picker, and grouped controls">
      <Example label="Input, label, and textarea">
        <div className="space-y-3"><div className="space-y-1.5"><Label htmlFor="component-name">Component name</Label><Input id="component-name" placeholder="e.g. Market snapshot" /></div><Textarea placeholder="Add a short note for this style pass..." /></div>
      </Example>
      <Example label="Input group">
        <div className="space-y-2"><InputGroup><InputGroupAddon><Search /></InputGroupAddon><InputGroupInput placeholder="Search components" /><InputGroupButton size="icon-sm" aria-label="Search"><ChevronRight /></InputGroupButton></InputGroup><InputGroup><InputGroupTextarea placeholder="Grouped multiline input" /><InputGroupAddon align="block-end"><InputGroupButton variant="outline">Attach</InputGroupButton></InputGroupAddon></InputGroup></div>
      </Example>
      <Example label="Select and checkbox">
        <div className="space-y-3"><Select value={selectValue} onValueChange={setSelectValue}><SelectTrigger><SelectValue placeholder="Choose a surface" /></SelectTrigger><SelectContent><SelectItem value="neutral">Neutral canvas</SelectItem><SelectItem value="muted">Muted surface</SelectItem><SelectItem value="accent">Accent surface</SelectItem></SelectContent></Select><label className="flex items-center gap-2 text-sm"><Checkbox checked={checked} onCheckedChange={(value) => setChecked(value === true)} />Use semantic tokens</label></div>
      </Example>
      <Example label="Calendar">
        <Calendar mode="single" selected={calendarDate} onSelect={setCalendarDate} className="rounded-md border" />
      </Example>
      <Example label="Date picker">
        <div className="flex flex-wrap items-center gap-2"><DatePicker value={date} onChange={setDate} disableWeekends={false} placeholder="Pick a date" /><span className="font-mono text-xs text-muted-foreground">{date?.toLocaleDateString() ?? "No date"}</span></div>
      </Example>
    </ShowcaseSection>
  )
}

function NavigationSection() {
  const [accordion, setAccordion] = useState("tokens")
  const [collapsibleOpen, setCollapsibleOpen] = useState(true)
  return (
    <ShowcaseSection title="Navigation & disclosure" description="Tabs, accordion, collapsible, breadcrumbs, command, and pagination">
      <Example label="Tabs" className="xl:col-span-2">
        <Tabs defaultValue="visual" className="w-full"><TabsList><TabsTrigger value="visual">Visual</TabsTrigger><TabsTrigger value="behavior">Behavior</TabsTrigger><TabsTrigger value="notes">Notes</TabsTrigger></TabsList><TabsContent value="visual" className="mt-3 text-xs text-muted-foreground">Inspect spacing, radius, type, and contrast here.</TabsContent><TabsContent value="behavior" className="mt-3 text-xs text-muted-foreground">Interactive states remain available for keyboard review.</TabsContent><TabsContent value="notes" className="mt-3 text-xs text-muted-foreground">A compact content region keeps the gallery scannable.</TabsContent></Tabs>
      </Example>
      <Example label="Accordion">
        <Accordion type="single" collapsible value={accordion} onValueChange={setAccordion}><AccordionItem value="tokens"><AccordionTrigger>Token mapping</AccordionTrigger><AccordionContent className="text-xs text-muted-foreground">Background, foreground, muted, border, and semantic accents are centralized.</AccordionContent></AccordionItem><AccordionItem value="density"><AccordionTrigger>Density rules</AccordionTrigger><AccordionContent className="text-xs text-muted-foreground">Compact controls use stable heights and predictable gaps.</AccordionContent></AccordionItem></Accordion>
      </Example>
      <Example label="Collapsible">
        <Collapsible open={collapsibleOpen} onOpenChange={setCollapsibleOpen}><CollapsibleTrigger asChild><Button variant="outline" size="sm">{collapsibleOpen ? "Hide" : "Show"} implementation note</Button></CollapsibleTrigger><CollapsibleContent className="mt-3 rounded-md bg-muted/45 p-3 text-xs text-muted-foreground">Keep page-private examples close to the route and promote only stable primitives.</CollapsibleContent></Collapsible>
      </Example>
      <Example label="Breadcrumb">
        <Breadcrumb><BreadcrumbList><BreadcrumbItem><BreadcrumbLink href="#">POC</BreadcrumbLink></BreadcrumbItem><BreadcrumbSeparator /><BreadcrumbItem><BreadcrumbEllipsis /></BreadcrumbItem><BreadcrumbSeparator /><BreadcrumbItem><BreadcrumbPage>Components</BreadcrumbPage></BreadcrumbItem></BreadcrumbList></Breadcrumb>
      </Example>
      <Example label="Command">
        <Command className="rounded-lg border shadow-none"><CommandInput placeholder="Search style tokens..." /><CommandList><CommandEmpty>No matching token.</CommandEmpty><CommandGroup heading="Suggestions"><CommandItem><CommandIcon />Open typography<CommandShortcut>⌘T</CommandShortcut></CommandItem><CommandItem><FileText />Inspect card<CommandShortcut>⌘K</CommandShortcut></CommandItem></CommandGroup><CommandSeparator /><CommandGroup heading="Actions"><CommandItem><Settings2 />Open settings</CommandItem></CommandGroup></CommandList></Command>
      </Example>
      <Example label="Pagination" className="xl:col-span-2">
        <Pagination><PaginationContent><PaginationItem><PaginationPrevious /></PaginationItem><PaginationItem><PaginationLink isActive>1</PaginationLink></PaginationItem><PaginationItem><PaginationLink>2</PaginationLink></PaginationItem><PaginationItem><PaginationEllipsis /></PaginationItem><PaginationItem><PaginationNext /></PaginationItem></PaginationContent></Pagination>
      </Example>
    </ShowcaseSection>
  )
}

function OverlaySection() {
  const [dialogOpen, setDialogOpen] = useState(false)
  const [confirmOpen, setConfirmOpen] = useState(false)
  const [popoverOpen, setPopoverOpen] = useState(false)
  return (
    <ShowcaseSection title="Overlays & menus" description="Dialog, drawer, sheet, popover, dropdown, context menu, and tooltip">
      <Example label="Dialog">
        <Dialog open={dialogOpen} onOpenChange={setDialogOpen}><DialogTrigger asChild><Button variant="outline"><CircleHelp />Open dialog</Button></DialogTrigger><DialogContent><DialogHeader><DialogTitle>Review component change</DialogTitle><DialogDescription>Confirm that this example is ready for the next visual pass.</DialogDescription></DialogHeader><DialogFooter><Button variant="ghost" onClick={() => setDialogOpen(false)}>Cancel</Button><Button onClick={() => setDialogOpen(false)}>Confirm</Button></DialogFooter></DialogContent></Dialog>
      </Example>
      <Example label="Drawer">
        <Drawer><DrawerTrigger asChild><Button variant="outline"><Inbox />Open drawer</Button></DrawerTrigger><DrawerContent><DrawerHeader><DrawerTitle>Detail drawer</DrawerTitle><DrawerDescription>Useful for inspecting a selected row without losing context.</DrawerDescription></DrawerHeader><div className="px-4 text-sm text-muted-foreground">Drawer content stays focused and vertically scrollable.</div><DrawerFooter><Button>Apply</Button></DrawerFooter></DrawerContent></Drawer>
      </Example>
      <Example label="Sheet">
        <Sheet><SheetTrigger asChild><Button variant="outline"><Settings2 />Open sheet</Button></SheetTrigger><SheetContent><SheetHeader><SheetTitle>Settings sheet</SheetTitle><SheetDescription>Use a sheet for secondary configuration on smaller screens.</SheetDescription></SheetHeader><div className="py-6 text-sm text-muted-foreground">Sheet content with a stable close path.</div><SheetFooter><Button>Save changes</Button></SheetFooter></SheetContent></Sheet>
      </Example>
      <Example label="Popover">
        <Popover open={popoverOpen} onOpenChange={setPopoverOpen}><PopoverTrigger asChild><Button variant="outline"><Filter />Filter popover</Button></PopoverTrigger><PopoverContent className="w-64"><div className="space-y-3"><p className="text-sm font-medium">Quick filter</p><div className="flex items-center gap-2 text-sm"><Checkbox defaultChecked />Only changed tokens</div><Button className="w-full" size="sm" onClick={() => setPopoverOpen(false)}>Apply filter</Button></div></PopoverContent></Popover>
      </Example>
      <Example label="Dropdown menu">
        <DropdownMenu><DropdownMenuTrigger asChild><Button variant="outline"><MoreHorizontal />Actions</Button></DropdownMenuTrigger><DropdownMenuContent align="start"><DropdownMenuLabel>Component</DropdownMenuLabel><DropdownMenuGroup><DropdownMenuItem><FileText />Inspect<DropdownMenuShortcut>⌘I</DropdownMenuShortcut></DropdownMenuItem><DropdownMenuItem><Bell />Notify</DropdownMenuItem></DropdownMenuGroup><DropdownMenuSeparator /><DropdownMenuCheckboxItem checked>Show preview</DropdownMenuCheckboxItem><DropdownMenuRadioGroup value="all"><DropdownMenuRadioItem value="all">All states</DropdownMenuRadioItem><DropdownMenuRadioItem value="active">Active only</DropdownMenuRadioItem></DropdownMenuRadioGroup><DropdownMenuSub><DropdownMenuSubTrigger>More options</DropdownMenuSubTrigger><DropdownMenuSubContent><DropdownMenuItem>Copy token</DropdownMenuItem><DropdownMenuItem>Open source</DropdownMenuItem></DropdownMenuSubContent></DropdownMenuSub></DropdownMenuContent></DropdownMenu>
      </Example>
      <Example label="Context menu">
        <ContextMenu><ContextMenuTrigger className="flex h-20 items-center justify-center rounded-md border border-dashed bg-muted/25 text-xs text-muted-foreground">Right click this surface</ContextMenuTrigger><ContextMenuContent><ContextMenuLabel>Selection</ContextMenuLabel><ContextMenuItem><FileText />Inspect<ContextMenuShortcut>⌘I</ContextMenuShortcut></ContextMenuItem><ContextMenuCheckboxItem checked>Show preview</ContextMenuCheckboxItem><ContextMenuSeparator /><ContextMenuRadioGroup value="one"><ContextMenuRadioItem value="one">One column</ContextMenuRadioItem><ContextMenuRadioItem value="two">Two columns</ContextMenuRadioItem></ContextMenuRadioGroup><ContextMenuSub><ContextMenuSubTrigger>Export</ContextMenuSubTrigger><ContextMenuSubContent><ContextMenuItem>Copy CSS</ContextMenuItem><ContextMenuItem>Copy JSX</ContextMenuItem></ContextMenuSubContent></ContextMenuSub></ContextMenuContent></ContextMenu>
      </Example>
      <Example label="Tooltip and confirmation">
        <div className="flex flex-wrap gap-2"><TooltipProvider><Tooltip><TooltipTrigger asChild><Button variant="ghost" size="icon" aria-label="Help" title="Help"><CircleHelp /></Button></TooltipTrigger><TooltipContent>Inspect this component</TooltipContent></Tooltip></TooltipProvider><Button variant="destructive" onClick={() => setConfirmOpen(true)}><Trash2 />Delete</Button><ConfirmDialog open={confirmOpen} onOpenChange={setConfirmOpen} title="Delete example?" description="This action demonstrates the shared confirmation flow." onConfirm={() => { notification.success({ title: "Example deleted", description: "The notification root is active." }) }} confirmText="Delete" cancelText="Keep" destructive /></div>
      </Example>
    </ShowcaseSection>
  )
}

function DataSection() {
  return (
    <ShowcaseSection title="Data & surfaces" description="Card, table, scroll area, skeleton, empty state, count-up, and chart primitives">
      <Example label="Card" className="xl:col-span-2">
        <Card className="shadow-none"><CardHeader><CardTitle>Market snapshot</CardTitle><CardDescription>A framed unit for a genuinely reusable data block.</CardDescription><CardAction><Badge variant="outline">Live</Badge></CardAction></CardHeader><CardContent className="font-mono text-2xl tabular-nums">3,428.19</CardContent><CardFooter className="text-xs text-muted-foreground">Updated 12 seconds ago</CardFooter></Card>
      </Example>
      <Example label="Table" className="xl:col-span-2">
        <div className="overflow-x-auto rounded-md border"><Table><TableCaption>Recent component checks</TableCaption><TableHeader><TableRow><TableHead>Surface</TableHead><TableHead>Status</TableHead><TableHead className="text-right">Score</TableHead></TableRow></TableHeader><TableBody><TableRow><TableCell>Button</TableCell><TableCell><Badge variant="secondary">Ready</Badge></TableCell><TableCell className="text-right font-mono">98</TableCell></TableRow><TableRow><TableCell>Dialog</TableCell><TableCell><Badge variant="outline">Review</Badge></TableCell><TableCell className="text-right font-mono">91</TableCell></TableRow></TableBody><TableFooter><TableRow><TableCell colSpan={2}>Average</TableCell><TableCell className="text-right font-mono">94.5</TableCell></TableRow></TableFooter></Table></div>
      </Example>
      <Example label="Scroll area">
        <ScrollArea className="h-28 rounded-md border p-3"><div className="space-y-2 text-xs text-muted-foreground">{Array.from({ length: 8 }, (_, index) => <div key={index} className="flex items-center justify-between border-b border-border/50 pb-2"><span>Token audit {index + 1}</span><span className="font-mono">0{index + 1}:24</span></div>)}</div></ScrollArea>
      </Example>
      <Example label="Skeleton and count-up">
        <div className="space-y-4"><div className="space-y-2"><Skeleton className="h-3 w-24" /><Skeleton className="h-8 w-full" /></div><div className="flex items-baseline gap-2"><CountUp to={12848} separator="," className="font-mono text-2xl font-semibold tabular-nums" /><span className="text-xs text-muted-foreground">tokens inspected</span></div></div>
      </Example>
      <Example label="Empty state">
        <Empty className="min-h-40 border"><EmptyHeader><EmptyMedia variant="icon"><Inbox /></EmptyMedia><EmptyTitle>No saved variants</EmptyTitle><EmptyDescription>Start a style pass to create the first comparison.</EmptyDescription></EmptyHeader><EmptyContent><Button size="sm"><Plus />Create variant</Button></EmptyContent></Empty>
      </Example>
      <Example label="Chart" className="xl:col-span-2">
        <ChartContainer config={chartConfig} className="min-h-[220px] w-full"><AreaChart accessibilityLayer data={chartData} margin={{ left: 0, right: 8, top: 8, bottom: 0 }}><CartesianGrid vertical={false} /><XAxis dataKey="month" tickLine={false} axisLine={false} tickMargin={8} /><YAxis tickLine={false} axisLine={false} tickMargin={8} width={28} /><RechartsTooltip cursor={false} content={<ChartTooltipContent indicator="line" />} /><Legend content={<ChartLegendContent />} /><Area dataKey="mobile" type="monotone" fill="var(--color-mobile)" fillOpacity={0.12} stroke="var(--color-mobile)" stackId="a" /><Area dataKey="desktop" type="monotone" fill="var(--color-desktop)" fillOpacity={0.08} stroke="var(--color-desktop)" stackId="a" /></AreaChart></ChartContainer>
      </Example>
      <Example label="Line chart">
        <ChartContainer config={chartConfig} className="min-h-[180px] w-full"><LineChart accessibilityLayer data={chartData}><CartesianGrid vertical={false} /><XAxis dataKey="month" tickLine={false} axisLine={false} /><RechartsTooltip content={<ChartTooltipContent />} /><Line dataKey="desktop" type="monotone" stroke="var(--color-desktop)" strokeWidth={2} dot={false} /></LineChart></ChartContainer>
      </Example>
    </ShowcaseSection>
  )
}

function NewComponentsSection() {
  const [alertDialogOpen, setAlertDialogOpen] = useState(false)
  const [comboValue, setComboValue] = useState("neutral")
  const [sliderValue, setSliderValue] = useState([48])

  return (
    <ShowcaseSection title="New York additions" description="The remaining installable shadcn primitives added from the registry">
      <Example label="Alert dialog">
        <AlertDialog open={alertDialogOpen} onOpenChange={setAlertDialogOpen}>
          <AlertDialogTrigger asChild><Button variant="destructive"><Trash2 />Remove token</Button></AlertDialogTrigger>
          <AlertDialogContent>
            <AlertDialogHeader>
              <AlertDialogMedia><ShieldCheck /></AlertDialogMedia>
              <AlertDialogTitle>Remove this token?</AlertDialogTitle>
              <AlertDialogDescription>This destructive confirmation keeps the user in context before the change is committed.</AlertDialogDescription>
            </AlertDialogHeader>
            <AlertDialogFooter><AlertDialogCancel>Cancel</AlertDialogCancel><AlertDialogAction onClick={() => notification.success({ title: "Token removed", description: "The alert dialog action completed." })}>Remove</AlertDialogAction></AlertDialogFooter>
          </AlertDialogContent>
        </AlertDialog>
      </Example>
      <Example label="Aspect ratio">
        <AspectRatio ratio={16 / 9} className="overflow-hidden rounded-md border bg-muted/35">
          <div className="flex size-full flex-col items-center justify-center gap-2 text-xs text-muted-foreground"><ImageIcon className="size-5" /><span>16:9 preview surface</span></div>
        </AspectRatio>
      </Example>
      <Example label="Attachment">
        <AttachmentGroup>
          <Attachment state="done">
            <AttachmentMedia><FileText /></AttachmentMedia>
            <AttachmentContent><AttachmentTitle>theme-tokens.json</AttachmentTitle><AttachmentDescription>18 KB · ready</AttachmentDescription></AttachmentContent>
            <AttachmentActions><AttachmentAction aria-label="Download attachment" title="Download attachment"><Download /></AttachmentAction></AttachmentActions>
          </Attachment>
          <Attachment state="uploading" size="sm">
            <AttachmentMedia><Upload /></AttachmentMedia>
            <AttachmentContent><AttachmentTitle>component-preview.png</AttachmentTitle><AttachmentDescription>Uploading...</AttachmentDescription></AttachmentContent>
          </Attachment>
        </AttachmentGroup>
      </Example>
      <Example label="Button group">
        <div className="space-y-3">
          <ButtonGroup><Button variant="outline"><PanelLeft />Layout</Button><ButtonGroupSeparator /><Button variant="outline">Compact</Button><Button variant="outline">Comfortable</Button></ButtonGroup>
          <ButtonGroup orientation="vertical"><ButtonGroupText>Export format</ButtonGroupText><Button variant="outline">CSS</Button><Button variant="outline">JSON</Button></ButtonGroup>
        </div>
      </Example>
      <Example label="Carousel">
        <div className="px-9">
          <Carousel opts={{ loop: true }}>
            <CarouselContent className="-ml-2">
              {["Neutral", "Muted", "Accent"].map((surface) => <CarouselItem key={surface} className="basis-3/4 pl-2"><div className="flex h-20 items-center justify-center rounded-md border bg-muted/35 text-sm font-medium">{surface} surface</div></CarouselItem>)}
            </CarouselContent>
            <CarouselPrevious aria-label="Previous surface" title="Previous surface" />
            <CarouselNext aria-label="Next surface" title="Next surface" />
          </Carousel>
        </div>
      </Example>
      <Example label="Combobox">
        <Combobox value={comboValue} onValueChange={(value) => setComboValue(value ?? "")}>
          <ComboboxInput placeholder="Choose a surface" showClear />
          <ComboboxContent><ComboboxEmpty>No surface found.</ComboboxEmpty><ComboboxList><ComboboxItem value="neutral">Neutral canvas</ComboboxItem><ComboboxItem value="muted">Muted surface</ComboboxItem><ComboboxItem value="accent">Accent surface</ComboboxItem></ComboboxList></ComboboxContent>
        </Combobox>
      </Example>
      <Example label="Direction">
        <DirectionProvider dir="rtl"><div className="flex items-center justify-between rounded-md border bg-muted/25 p-3 text-sm"><span>RTL preview</span><ChevronRight className="rotate-180" /></div></DirectionProvider>
      </Example>
      <Example label="Field layout">
        <FieldSet className="gap-3 rounded-md border p-3"><FieldLegend variant="label">Review settings</FieldLegend><FieldGroup className="gap-3"><Field orientation="horizontal"><FieldLabel htmlFor="field-name">Name</FieldLabel><FieldContent><Input id="field-name" defaultValue="Market snapshot" /><FieldDescription>Shown in the component inventory.</FieldDescription></FieldContent></Field><FieldSeparator>or</FieldSeparator><Field orientation="horizontal"><Checkbox id="field-notify" defaultChecked /><FieldLabel htmlFor="field-notify">Notify reviewers</FieldLabel></Field></FieldGroup></FieldSet>
      </Example>
      <Example label="Hover card">
        <HoverCard><HoverCardTrigger asChild><Button variant="link" className="px-0">Inspect token metadata</Button></HoverCardTrigger><HoverCardContent><div className="space-y-2"><p className="text-sm font-medium">primary / foreground</p><p className="text-xs leading-5 text-muted-foreground">Semantic tokens keep the gallery aligned with the application theme.</p><Badge variant="secondary">#fafafa</Badge></div></HoverCardContent></HoverCard>
      </Example>
      <Example label="Input OTP">
        <InputOTP maxLength={6} defaultValue="482910" aria-label="Verification code"><InputOTPGroup><InputOTPSlot index={0} /><InputOTPSlot index={1} /><InputOTPSlot index={2} /></InputOTPGroup><InputOTPSeparator /><InputOTPGroup><InputOTPSlot index={3} /><InputOTPSlot index={4} /><InputOTPSlot index={5} /></InputOTPGroup></InputOTP>
      </Example>
      <Example label="Item group">
        <ItemGroup className="rounded-md border"><Item variant="muted" size="sm"><ItemMedia variant="icon"><CheckCircle2 /></ItemMedia><ItemContent><ItemTitle>Token audit complete</ItemTitle><ItemDescription>All semantic roles have a rendered example.</ItemDescription></ItemContent><ItemActions><Badge variant="outline">Ready</Badge></ItemActions></Item><ItemSeparator /><Item size="sm"><ItemMedia variant="icon"><FileText /></ItemMedia><ItemContent><ItemTitle>Review notes</ItemTitle><ItemDescription>2 pending visual decisions</ItemDescription></ItemContent></Item></ItemGroup>
      </Example>
      <Example label="Keyboard hint">
        <div className="flex items-center gap-3 rounded-md border bg-muted/25 p-3 text-sm"><span>Open command menu</span><KbdGroup><Kbd>Ctrl</Kbd><span className="text-muted-foreground">+</span><Kbd>K</Kbd></KbdGroup></div>
      </Example>
      <Example label="Menubar">
        <Menubar className="w-fit"><MenubarMenu><MenubarTrigger>File</MenubarTrigger><MenubarContent><MenubarLabel>Workspace</MenubarLabel><MenubarItem><FileText />New variant<MenubarShortcut>Ctrl+N</MenubarShortcut></MenubarItem><MenubarItem><Download />Export</MenubarItem><MenubarSeparator /><MenubarCheckboxItem checked>Show preview</MenubarCheckboxItem><MenubarRadioGroup value="css"><MenubarRadioItem value="css">CSS output</MenubarRadioItem><MenubarRadioItem value="json">JSON output</MenubarRadioItem></MenubarRadioGroup></MenubarContent></MenubarMenu><MenubarMenu><MenubarTrigger>View</MenubarTrigger><MenubarContent><MenubarItem>Tokens</MenubarItem><MenubarSub><MenubarSubTrigger>Density</MenubarSubTrigger><MenubarSubContent><MenubarItem>Compact</MenubarItem><MenubarItem>Comfortable</MenubarItem></MenubarSubContent></MenubarSub></MenubarContent></MenubarMenu></Menubar>
      </Example>
      <Example label="Native select">
        <NativeSelect defaultValue="snapshot" aria-label="Select output mode"><NativeSelectOption value="snapshot">Snapshot</NativeSelectOption><NativeSelectOptGroup label="Live modes"><NativeSelectOption value="live">Live preview</NativeSelectOption><NativeSelectOption value="review">Review mode</NativeSelectOption></NativeSelectOptGroup></NativeSelect>
      </Example>
      <Example label="Navigation menu" className="xl:col-span-2">
        <NavigationMenu className="max-w-full justify-start"><NavigationMenuList className="justify-start"><NavigationMenuItem><NavigationMenuTrigger>Components</NavigationMenuTrigger><NavigationMenuContent><div className="grid w-[320px] gap-2 p-2 sm:w-[420px] sm:grid-cols-2"><NavigationMenuLink href="#"><span className="font-medium">Foundations</span><span className="text-xs text-muted-foreground">Colors and typography</span></NavigationMenuLink><NavigationMenuLink href="#"><span className="font-medium">Interactions</span><span className="text-xs text-muted-foreground">Focus and keyboard states</span></NavigationMenuLink></div></NavigationMenuContent></NavigationMenuItem><NavigationMenuItem><NavigationMenuLink href="#">Documentation</NavigationMenuLink></NavigationMenuItem></NavigationMenuList></NavigationMenu>
      </Example>
      <Example label="Radio group">
        <RadioGroup defaultValue="neutral" className="gap-2"><div className="flex items-center gap-2"><RadioGroupItem value="neutral" id="radio-neutral" /><Label htmlFor="radio-neutral">Neutral</Label></div><div className="flex items-center gap-2"><RadioGroupItem value="muted" id="radio-muted" /><Label htmlFor="radio-muted">Muted</Label></div><div className="flex items-center gap-2"><RadioGroupItem value="accent" id="radio-accent" /><Label htmlFor="radio-accent">Accent</Label></div></RadioGroup>
      </Example>
      <Example label="Resizable panels" className="xl:col-span-2">
        <ResizablePanelGroup orientation="horizontal" className="min-h-28 rounded-md border"><ResizablePanel defaultSize={36}><div className="flex h-full items-center justify-center bg-muted/25 p-3 text-sm">Preview</div></ResizablePanel><ResizableHandle withHandle /><ResizablePanel defaultSize={64}><div className="flex h-full items-center justify-center p-3 text-sm text-muted-foreground">Inspector</div></ResizablePanel></ResizablePanelGroup>
      </Example>
      <Example label="Slider">
        <div className="space-y-3"><Slider value={sliderValue} onValueChange={setSliderValue} max={100} step={1} aria-label="Radius" /><div className="flex justify-between text-xs text-muted-foreground"><span>Radius</span><span className="font-mono">{sliderValue[0]}%</span></div></div>
      </Example>
      <Example label="Switch">
        <div className="flex items-center justify-between rounded-md border p-3"><div><p className="text-sm font-medium">Live preview</p><p className="text-xs text-muted-foreground">Refresh the example as tokens change.</p></div><Switch defaultChecked aria-label="Toggle live preview" /></div>
      </Example>
    </ShowcaseSection>
  )
}

function MessagingSection() {
  return (
    <ShowcaseSection title="Messages & assistant surfaces" description="Bubble, message anatomy, message scroller, avatar, and notification feedback">
      <Example label="Bubble group">
        <BubbleGroup><Bubble variant="tinted"><BubbleContent>Use the tinted bubble for an assistant response.</BubbleContent><BubbleReactions><Button variant="ghost" size="icon-xs" aria-label="Helpful"><Check /></Button></BubbleReactions></Bubble><Bubble variant="outline" align="end"><BubbleContent>And the outline variant for a user reply.</BubbleContent></Bubble></BubbleGroup>
      </Example>
      <Example label="Message anatomy">
        <MessageGroup><Message><MessageAvatar><Avatar className="size-8"><AvatarFallback>AI</AvatarFallback></Avatar></MessageAvatar><MessageContent><MessageHeader>Assistant · just now</MessageHeader><Bubble variant="muted"><BubbleContent>Message primitives keep avatar, header, body, and footer independently tunable.</BubbleContent></Bubble><MessageFooter>Generated locally</MessageFooter></MessageContent></Message></MessageGroup>
      </Example>
      <Example label="Message scroller">
        <div className="h-52 rounded-md border p-2"><MessageScrollerProvider defaultScrollPosition="end"><MessageScroller><MessageScrollerViewport aria-label="Component messages"><MessageScrollerContent>{["Inspect spacing", "Tune contrast", "Verify focus"].map((item) => <MessageScrollerItem key={item} messageId={item}><div className="rounded-md bg-muted/50 p-2 text-xs">{item}</div></MessageScrollerItem>)}</MessageScrollerContent></MessageScrollerViewport><MessageScrollerButton direction="start" aria-label="Scroll to start" /></MessageScroller></MessageScrollerProvider></div>
      </Example>
      <Example label="Avatar group">
        <div className="flex items-center gap-4"><AvatarGroup><Avatar><AvatarImage src="/avatars/shadcn.jpg" alt="Workspace owner" /><AvatarFallback>CC</AvatarFallback><AvatarBadge /></Avatar><Avatar><AvatarFallback>AI</AvatarFallback></Avatar><AvatarGroupCount>+4</AvatarGroupCount></AvatarGroup><span className="text-xs text-muted-foreground">Active reviewers</span></div>
      </Example>
      <Example label="Notifications">
        <div className="flex flex-wrap gap-2"><Button size="sm" onClick={() => notification.success({ title: "Saved", description: "The component variant is ready." })}><Check />Success</Button><Button size="sm" variant="outline" onClick={() => notification.info({ title: "Snapshot mode", description: "This page uses local examples only." })}><Bell />Info</Button><Button size="sm" variant="secondary" onClick={() => notification.warn({ title: "Review spacing", description: "The selected surface needs another pass." })}>Warn</Button></div>
      </Example>
    </ShowcaseSection>
  )
}

function SidebarSection() {
  return (
    <ShowcaseSection title="Sidebar anatomy" description="The shared sidebar primitives used by the application shell">
      <Example label="Sidebar primitives" className="md:col-span-2 xl:col-span-3">
        <SidebarProvider defaultOpen className="min-h-[300px] overflow-hidden rounded-md border bg-muted/20">
          <Sidebar collapsible="none" className="relative h-[300px] w-[220px] shrink-0 border-r">
            <SidebarHeader><div className="flex items-center gap-2 px-2 py-1 text-sm font-semibold"><div className="flex size-6 items-center justify-center rounded-md bg-primary text-primary-foreground"><Sparkles className="size-3.5" /></div>Style Lab</div><SidebarInput placeholder="Filter sections" /></SidebarHeader>
            <SidebarSeparator />
            <SidebarContent><SidebarGroup><SidebarGroupLabel>Workspace</SidebarGroupLabel><SidebarGroupAction aria-label="Add workspace"><Plus /></SidebarGroupAction><SidebarGroupContent><SidebarMenu><SidebarMenuItem><SidebarMenuButton isActive tooltip="Components"><CommandIcon /><span>Components</span></SidebarMenuButton><SidebarMenuAction showOnHover aria-label="Component actions"><MoreHorizontal /></SidebarMenuAction><SidebarMenuBadge>46</SidebarMenuBadge></SidebarMenuItem><SidebarMenuItem><SidebarMenuButton tooltip="Tokens"><Zap /><span>Tokens</span></SidebarMenuButton><SidebarMenuSub><SidebarMenuSubItem><SidebarMenuSubButton href="#" isActive><span>Color</span></SidebarMenuSubButton></SidebarMenuSubItem><SidebarMenuSubItem><SidebarMenuSubButton href="#"><span>Type</span></SidebarMenuSubButton></SidebarMenuSubItem></SidebarMenuSub></SidebarMenuItem></SidebarMenu></SidebarGroupContent></SidebarGroup><SidebarGroup><SidebarGroupLabel>Recent</SidebarGroupLabel><SidebarGroupContent><SidebarMenu><SidebarMenuSkeleton showIcon /><SidebarMenuItem><SidebarMenuButton><FileText /><span>Review notes</span></SidebarMenuButton></SidebarMenuItem></SidebarMenu></SidebarGroupContent></SidebarGroup></SidebarContent>
            <SidebarFooter><div className="flex items-center gap-2 rounded-md bg-sidebar-accent p-2 text-xs"><Avatar className="size-7"><AvatarFallback>CC</AvatarFallback></Avatar><div className="min-w-0 flex-1"><div className="truncate font-medium">cccs7</div><div className="truncate text-muted-foreground">Workspace owner</div></div><UserRound className="size-3.5 text-muted-foreground" /></div></SidebarFooter>
          </Sidebar>
          <div className="flex min-w-0 flex-1 flex-col gap-3 p-4"><div className="flex items-center gap-2 text-sm font-medium"><Terminal className="size-4 text-muted-foreground" />Preview surface</div><Separator /><p className="max-w-md text-xs leading-5 text-muted-foreground">This isolated sidebar uses the same primitives as the global shell, making collapsed, active, badge, skeleton, and nested menu states easy to tune.</p></div>
        </SidebarProvider>
      </Example>
    </ShowcaseSection>
  )
}

export default function ShadcnPocPage() {
  return (
    <WorkspaceShell sectionLabel="POC" sectionUrl="/poc" pageTitle="shadcn Components">
      <TooltipProvider>
        <div className="space-y-5">
          <div className="flex flex-col gap-4 rounded-lg border border-border/70 bg-muted/20 p-5 md:flex-row md:items-end md:justify-between">
            <div className="min-w-0 space-y-3">
              <div className="flex flex-wrap items-center gap-2"><Badge variant="secondary" className="rounded-md"><Sparkles className="size-3.5" />POC</Badge><Badge variant="outline" className="rounded-md bg-background">New York primitives</Badge></div>
              <div className="space-y-2"><h1 className="text-2xl font-semibold tracking-tight text-foreground sm:text-3xl">shadcn Components</h1><p className="max-w-3xl text-sm leading-6 text-muted-foreground">A working surface for tuning the shared component language. Every primitive is rendered below with a realistic state and a keyboard-friendly interaction.</p></div>
            </div>
            <div className="grid shrink-0 grid-cols-2 gap-x-6 gap-y-2 text-xs"><div><div className="text-muted-foreground">Primitive files</div><div className="font-mono text-lg font-semibold tabular-nums">63</div></div><div><div className="text-muted-foreground">Interactive sets</div><div className="font-mono text-lg font-semibold tabular-nums">13</div></div></div>
          </div>
          <div className="flex items-center gap-2 text-xs text-muted-foreground"><CalendarDays className="size-3.5" />Local component inventory <Separator orientation="vertical" className="h-3.5" />Use this page as the visual tuning baseline.</div>
          <div className="space-y-4"><ButtonAndStatusSection /><FormSection /><NavigationSection /><OverlaySection /><DataSection /><NewComponentsSection /><MessagingSection /><SidebarSection /></div>
        </div>
      </TooltipProvider>
    </WorkspaceShell>
  )
}

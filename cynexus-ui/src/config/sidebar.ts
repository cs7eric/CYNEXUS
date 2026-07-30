import {
  Activity,
  AudioWaveform,
  ComputerIcon,
  Download,
  FileText,
  Flame,
  GalleryVerticalEnd,
  LayoutDashboard,
  LineChart,
  Settings,
  BrainCircuit,
  Database,
  Beaker,
  Server,
  Newspaper,
  WalletCards,
  Camera,
  Sparkles,
} from "lucide-react"

export const sidebarConfig = {
  user: {
    name: "cccs7",
    email: "csq020611@gmail.com",
    avatar: "/avatars/shadcn.jpg",
  },
  teams: [
    {
      name: "Application Hub",
      logo: GalleryVerticalEnd,
      plan: "Workspace",
    },
    {
      name: "Media Tools",
      logo: AudioWaveform,
      plan: "Pipeline",
    },
  ],
  navMain: [
    {
      title: "Cynexus Agent",
      url: "/cynexus-agent",
      icon: Sparkles,
    },
    {
      title: "Finance",
      url: "/finance/overview",
      icon: WalletCards,
      items: [
        { title: "Overview", url: "/finance/overview" },
        { title: "Ledger", url: "/finance/ledger" },
        { title: "Transactions", url: "/finance/transactions" },
        { title: "Accounts", url: "/finance/accounts" },
        { title: "AI Capture", url: "/finance/ai-capture" },
      ],
    },
    {
      title: "Dashboard",
      url: "/dashboard",
      icon: LayoutDashboard,
    },
    {
      title: "Parsing Tool",
      url: "/mp4-to-word",
      icon: FileText,
      items: [
        {
          title: "open",
          url: "/mp4-to-word",
        },
        {
          title: "history",
          url: "/mp4-to-word/history",
        },
        {
          title: "process history",
          url: "/mp4-to-word/process-history",
        },
        {
          title: "downloader",
          url: "/downloader",
        },
      ],
    },
    {
      title: "market",
      url: "/market/pulse",
      icon: Activity,
      items: [
        {
          title: "market pulse",
          url: "/market/pulse",
        },
        {
          title: "market sentiment",
          url: "/market/sentiment",
        },
        {
          title: "market-margin-trading",
          url: "/market/margin-trading",
        },
        {
          title: "market-etf",
          url: "/market/etf",
        },
        {
          title: "futures position",
          url: "/market/futures-position",
        },
      ],
    },
    {
      title: "Information",
      url: "/information",
      icon: Newspaper,
      items: [
        {
          title: "Feed",
          url: "/information",
        },
        {
          title: "AI analyzed",
          url: "/information/analyzed",
        },
      ],
    },
    {
      title: "Stock Overview",
      url: "/stock-overview",
      icon: LineChart,
      items: [
        {
          title: "overview",
          url: "/stock-overview",
        },
        {
          title: "market",
          url: "/stock-overview/market",
        },
        {
          title: "analysis",
          url: "/stock-overview/application-analysis",
        },
        {
          title: "industry",
          url: "/stock-overview/industry-application",
        },
        {
          title: "self-selected",
          url: "/stock-overview/self-selected",
        },
      ],
    },
    {
      title: "Stock Review",
      url: "/stock-review",
      icon: LineChart,
      items: [
        {
          title: "open",
          url: "/stock-review",
        },
      ],
    },
    {
      title: "Knowledge Base",
      url: "/knowledge-base/view",
      icon: ComputerIcon,
      items: [
        {
          title: "writing",
          url: "/knowledge-base/writing",
        },
        {
          title: "view",
          url: "/knowledge-base/view",
        },
      ],
    },
    {
      title: "AI",
      url: "/ai",
      icon: BrainCircuit,
      items: [
        {
          title: "Provider",
          url: "/ai/provider",
        },
        {
          title: "Prompt",
          url: "/ai/prompt",
        },
        {
          title: "Usage",
          url: "/ai/usage",
        },
      ],
    },
    {
      title: "Settings",
      url: "/settings/scheduler",
      icon: Settings,
      items: [
        {
          title: "Scheduler",
          url: "/settings/scheduler",
        },
        {
          title: "Data Source",
          url: "/settings/data-source",
        },
        {
          title: "Provider",
          url: "/settings/provider",
        },
      ],
    },
    {
      title: "POC",
      url: "/poc",
      icon: Beaker,
      items: [
        {
          title: "workspace",
          url: "/poc",
        },
        {
          title: "ai poc",
          url: "/poc/ai",
        },
        {
          title: "a stock skill",
          url: "/poc/a-stock-skill",
        },
        {
          title: "markdown render",
          url: "/poc/markdown-render",
        },
        {
          title: "easy_tdx data lab",
          url: "/poc/easy-tdx",
        },
      ],
    },
  ],
  projects: [
    {
      name: "Dashboard",
      url: "/dashboard",
      icon: LayoutDashboard,
    },
    {
      name: "Finance Ledger",
      url: "/finance/ledger",
      icon: WalletCards,
    },
    {
      name: "Finance AI Capture",
      url: "/finance/ai-capture",
      icon: Camera,
    },
    {
      name: "Downloader",
      url: "/downloader",
      icon: Download,
    },
    {
      name: "Parsing Tool",
      url: "/mp4-to-word",
      icon: FileText,
    },
    {
      name: "MP4 History",
      url: "/mp4-to-word/history",
      icon: FileText,
    },
    {
      name: "MP4 Process History",
      url: "/mp4-to-word/process-history",
      icon: FileText,
    },
    {
      name: "Stock Overview",
      url: "/stock-overview",
      icon: LineChart,
    },
    {
      name: "Market Pulse",
      url: "/stock-overview/market",
      icon: Flame,
    },
    {
      name: "Market Margin Trading",
      url: "/market/margin-trading",
      icon: Activity,
    },
    {
      name: "Market Information",
      url: "/information",
      icon: Newspaper,
    },
    {
      name: "Information · AI analyzed",
      url: "/information/analyzed",
      icon: BrainCircuit,
    },
    {
      name: "Application Analysis",
      url: "/stock-overview/application-analysis",
      icon: LineChart,
    },
    {
      name: "Industry / Concept",
      url: "/stock-overview/industry-application",
      icon: LineChart,
    },
    {
      name: "Stock Review",
      url: "/stock-review",
      icon: LineChart,
    },
    {
      name: "POC",
      url: "/poc",
      icon: Beaker,
    },
    {
      name: "POC · AI",
      url: "/poc/ai",
      icon: BrainCircuit,
    },
    {
      name: "POC · A股 Skill",
      url: "/poc/a-stock-skill",
      icon: Database,
    },
    {
      name: "POC · Markdown Render",
      url: "/poc/markdown-render",
      icon: FileText,
    },
    {
      name: "POC · easy_tdx Data Lab",
      url: "/poc/easy-tdx",
      icon: Database,
    },
    {
      name: "Settings · Scheduler",
      url: "/settings/scheduler",
      icon: Settings,
    },
    {
      name: "Cynexus Agent",
      url: "/cynexus-agent",
      icon: Sparkles,
    },
    {
      name: "AI · Provider",
      url: "/ai/provider",
      icon: BrainCircuit,
    },
    {
      name: "Settings · Data Source",
      url: "/settings/data-source",
      icon: Database,
    },
    {
      name: "Settings · Provider",
      url: "/settings/provider",
      icon: Server,
    },
    {
      name: "AI · Prompt",
      url: "/ai/prompt",
      icon: BrainCircuit,
    },
    {
      name: "AI · Usage",
      url: "/ai/usage",
      icon: Activity,
    },
    {
      name: "Knowledge Base",
      url: "/knowledge-base",
      icon: ComputerIcon,
    },
  ],
}

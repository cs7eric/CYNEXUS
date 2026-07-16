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
      title: "Dashboard",
      url: "/dashboard",
      icon: LayoutDashboard,
    },
    {
      title: "MP4 to Word",
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
      ],
    },
    {
      title: "Information",
      url: "/information",
      icon: Newspaper,
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
      title: "Settings",
      url: "/settings/scheduler",
      icon: Settings,
      items: [
        {
          title: "Scheduler",
          url: "/settings/scheduler",
        },
        {
          title: "AI Provider",
          url: "/settings/ai-provider",
        },
        {
          title: "Data Source",
          url: "/settings/data-source",
        },
        {
          title: "Provider",
          url: "/settings/provider",
        },
        {
          title: "Prompt",
          url: "/settings/prompt",
        },
        {
          title: "AI Usage",
          url: "/settings/ai-usage",
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
      name: "Downloader",
      url: "/downloader",
      icon: Download,
    },
    {
      name: "MP4 to Word",
      url: "/mp4-to-word",
      icon: FileText,
    },
    {
      name: "MP4 History",
      url: "/mp4-to-word/history",
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
      name: "Settings · Scheduler",
      url: "/settings/scheduler",
      icon: Settings,
    },
    {
      name: "Settings · AI Provider",
      url: "/settings/ai-provider",
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
      name: "Settings · Prompt",
      url: "/settings/prompt",
      icon: BrainCircuit,
    },
    {
      name: "Settings · AI Usage",
      url: "/settings/ai-usage",
      icon: Activity,
    },
    {
      name: "Knowledge Base",
      url: "/knowledge-base",
      icon: ComputerIcon,
    },
  ],
}

/**
 * Design entry:
 * - Data/API: transcribe, parse-video, task snapshot, SSE stream, mp4-history
 * - Front design: design/front/mp4-to-word.md
 * - Backend design: design/backend/mp4-history-reference-flow.md
 * - Change rule: review design before edits; sync design if remote intake, SSE, or history flow changes.
 */
import { useRef, useState, useCallback, useEffect, useMemo, type ReactNode } from "react";
import { Link, useSearchParams } from "react-router-dom";
import type { Phase, PostMetadata, SSEEvent, TransferProgress } from "../../lib/types";
import { askQuestion, createSSEConnection, exportMarkdown, fetchTaskSnapshot, saveMP4History, sendDownloaderResultToParse, uploadFile, uploadFileWithProgress } from "@/services/core/transcription";
import { notification } from "@/components/ui/notification";
import { QA_STYLE_FIX } from "./styles";
import { buildSummaryCards } from "./lib/summary-renderer";
import { renderQaAnswer } from "./lib/qa-renderer";
import { AskSection } from "@/components/ask-section";
import { FloatingAskBar } from "@/components/floating-ask-bar";
import { ReaderModal } from "@/components/reader-modal";
import { WorkspaceShell } from "@/layout/workspace-shell";
import { Alert, AlertDescription, AlertTitle } from "@/components/ui/alert";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Separator } from "@/components/ui/separator";
import { Skeleton } from "@/components/ui/skeleton";
import type { DownloaderParseData, RemoteParsePayload } from "@/services/legacy/downloader";
import { parseDownloaderUrl } from "@/services/legacy/downloader";
import { LinkActionRow } from "@/views/downloader/components/link-action-row";
import { LoadingState } from "@/views/downloader/components/loading-state";
import { formatDuration } from "@/views/downloader/lib/format";
import { AlertCircle, BookOpen, Captions, CheckCircle2, Code2, Copy, Download, ExternalLink, FileAudio, FileText, History, Info, LinkIcon, ListChecks, Loader2, RefreshCw, Send, Sparkles, UploadCloud, Video, X } from "lucide-react";

interface QaItem {
  id: string;
  question: string;
  answerHtml?: string;
  loading?: boolean;
}

interface RemoteTaskDraft {
  downloadUrl: string;
  title: string;
  sourceUrl: string;
  platform: string;
  duration?: number;
  noteType: string;
  audioUrl: string;
}

function formatBytes(value?: number) {
  if (!value || value <= 0) return "-";
  const units = ["B", "KB", "MB", "GB"];
  let current = value;
  let index = 0;
  while (current >= 1024 && index < units.length - 1) {
    current /= 1024;
    index += 1;
  }
  return `${current >= 100 || index === 0 ? current.toFixed(0) : current.toFixed(1)} ${units[index]}`;
}

function formatDurationSeconds(value?: number | null) {
  if (value == null || Number.isNaN(value) || value < 0) return "--";
  const total = Math.floor(value);
  const minutes = Math.floor(total / 60);
  const seconds = total % 60;
  if (minutes > 0) return `${minutes}m ${String(seconds).padStart(2, "0")}s`;
  return `${seconds}s`;
}

function formatMediaDuration(value?: number) {
  if (!value || Number.isNaN(value)) return "--";
  const total = Math.max(0, Math.floor(value));
  const h = Math.floor(total / 3600);
  const m = Math.floor((total % 3600) / 60);
  const s = total % 60;
  if (h > 0) {
    return [h, m, s].map((part, index) => (index === 0 ? String(part) : String(part).padStart(2, "0"))).join(":");
  }
  return [m, s].map((part) => String(part).padStart(2, "0")).join(":");
}

function compactUrl(value: string) {
  if (!value) return "--";
  try {
    const url = new URL(value);
    const tail = url.pathname.split("/").filter(Boolean).pop() || "";
    return `${url.hostname}${tail ? ` / ${tail.slice(0, 36)}` : ""}`;
  } catch {
    return value.length > 56 ? `${value.slice(0, 56)}...` : value;
  }
}

function hasTransferProgressSignal(progress?: TransferProgress | null) {
  return Boolean(
    progress &&
      (progress.phase ||
        progress.progress != null ||
        progress.downloaded_bytes != null ||
        progress.total_bytes != null ||
        progress.processed_bytes != null)
  );
}

function shouldKeepCompletedProgress(current: TransferProgress, next?: TransferProgress | null) {
  if ((current.progress || 0) < 100 || !next) return false;
  const nextProgress = next.progress ?? 0;
  const nextTotal = next.total_bytes ?? 0;
  const nextDownloaded = next.downloaded_bytes ?? 0;
  const nextProcessed = next.processed_bytes ?? 0;
  return nextProgress <= 0 && nextTotal <= 0 && nextDownloaded <= 0 && nextProcessed <= 0;
}

function selectDownloaderMediaUrl(result: DownloaderParseData | null) {
  if (!result) return "";
  return (
    result.originDownloadAudioUrl ||
    result.originDownloadVideoUrl ||
    result.downloadVideoUrl ||
    result.downloadAudioUrl ||
    ""
  );
}

function TextSkeletonBlock({ rows = 8 }: { rows?: number }) {
  return (
    <div className="space-y-3 p-1">
      {Array.from({ length: rows }).map((_, index) => (
        <Skeleton
          key={index}
          className={`h-4 bg-[#e4e4e7] ${index % 4 === 3 ? "w-2/3" : index % 3 === 2 ? "w-4/5" : "w-full"}`}
        />
      ))}
    </div>
  );
}

function MetricTile({
  icon,
  label,
  value,
  hint,
}: {
  icon: ReactNode;
  label: string;
  value: ReactNode;
  hint?: ReactNode;
}) {
  return (
    <div className="min-w-0 rounded-md bg-muted/60 px-4 py-3">
      <div className="flex items-center gap-2 text-xs font-medium text-muted-foreground">
        {icon}
        {label}
      </div>
      <div className="mt-2 truncate text-xl font-semibold tabular-nums text-foreground">{value}</div>
      {hint ? <div className="mt-1 truncate text-xs text-muted-foreground">{hint}</div> : null}
    </div>
  );
}

function ProgressCard({
  title,
  description,
  progress,
  transferred,
  total,
  eta,
  speed,
}: {
  title: string;
  description: string;
  progress: TransferProgress;
  transferred: number;
  total: number;
  eta?: number | null;
  speed?: number;
}) {
  const percent = Math.max(0, Math.min(100, Math.round(progress.progress || 0)));
  const status = percent >= 100 ? "Done" : progress.phase || "Running";
  const done = percent >= 100;
  const ProgressIcon = title === "Download" ? Download : title.includes("Upload") ? UploadCloud : RefreshCw;

  if (done) {
    return (
      <Card className="rounded-md border-0 bg-[#f9f9f9] py-2 shadow-none">
        <CardContent className="flex flex-col gap-1 px-2.5 py-1.5 sm:flex-row sm:items-center sm:justify-between">
          <div className="flex min-w-0 items-center gap-2">
            <span className="inline-flex size-6 shrink-0 items-center justify-center text-muted-foreground">
              <ProgressIcon className="size-3.5" />
            </span>
            <div className="flex min-w-0 flex-col gap-0.5 sm:flex-row sm:items-center sm:gap-2">
              <span className="truncate text-sm font-semibold text-foreground">{title}</span>
              <Badge variant="secondary" className="h-5 w-fit border-0 bg-background/80 px-1.5 text-[10px] font-medium">
                Done
              </Badge>
              <span className="truncate text-xs text-muted-foreground">
                {formatBytes(transferred)} / {formatBytes(total)}
              </span>
            </div>
          </div>
          <div className="flex shrink-0 flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
            <span className="rounded bg-background/75 px-2 py-0.5 font-medium tabular-nums text-foreground">100%</span>
            <span className="hidden h-3 w-px bg-border/70 sm:block" />
            <span>Total {formatBytes(total)}</span>
            <span className="h-3 w-px bg-border/70" />
            <span>ETA 0s</span>
          </div>
        </CardContent>
      </Card>
    );
  }

  return (
    <Card className="overflow-hidden rounded-md border-0 bg-[#f9f9f9] py-2 shadow-none">
      <CardHeader className="gap-3 pb-3">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0 space-y-2">
            <CardTitle className="flex items-center gap-2 text-base">
              <span className="inline-flex size-7 items-center justify-center text-muted-foreground">
                <ProgressIcon className="size-4" />
              </span>
              {title}
            </CardTitle>
            <CardDescription className="leading-6">{description}</CardDescription>
          </div>
          <Badge variant={percent >= 100 ? "default" : "secondary"}>{status}</Badge>
        </div>
      </CardHeader>
      <CardContent className="space-y-5">
        <div className="space-y-2">
          <div className="flex items-end justify-between">
            <span className="text-2xl font-semibold text-foreground">{percent}%</span>
            <span className="text-xs text-muted-foreground">{formatBytes(transferred)} / {formatBytes(total)}</span>
          </div>
          <Progress value={percent} className="h-2" />
        </div>
        <div className="grid grid-cols-3 gap-2 rounded-md bg-background/70 p-3">
          <div>
            <div className="text-[11px] font-medium uppercase text-muted-foreground">Speed</div>
            <div className="mt-1 text-sm font-medium text-foreground">{speed ? `${formatBytes(speed)}/s` : "--"}</div>
          </div>
          <div>
            <div className="text-[11px] font-medium uppercase text-muted-foreground">ETA</div>
            <div className="mt-1 text-sm font-medium text-foreground">{formatDurationSeconds(eta)}</div>
          </div>
          <div>
            <div className="text-[11px] font-medium uppercase text-muted-foreground">Total</div>
            <div className="mt-1 text-sm font-medium text-foreground">{formatBytes(total)}</div>
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export default function Mp4ToWordPage() {
  const fileInputRef = useRef<HTMLInputElement>(null);
  const esRef = useRef<EventSource | null>(null);
  const remoteStartKeyRef = useRef("");
  const transcriptTargetRef = useRef("");
  const transcriptFrameRef = useRef<number | null>(null);
  const transcriptLastFrameAtRef = useRef(0);
  const transcriptTypingCarryRef = useRef(0);
  const transcriptSeqRef = useRef(0);
  const [searchParams, setSearchParams] = useSearchParams();
  const initialTaskId = searchParams.get("task") || "";

  const initialRemoteDraft = useMemo<RemoteTaskDraft | null>(() => {
    const mode = searchParams.get("mode");
    const downloadUrl = searchParams.get("downloadUrl") || "";
    if (mode !== "remote" || !downloadUrl) return null;
    const durationParam = searchParams.get("duration");
    return {
      downloadUrl,
      title: searchParams.get("title") || "未命名视频",
      sourceUrl: searchParams.get("sourceUrl") || downloadUrl,
      platform: searchParams.get("platform") || "Downloader",
      duration: durationParam ? Number(durationParam) : undefined,
      noteType: searchParams.get("noteType") || "video",
      audioUrl: searchParams.get("audioUrl") || "",
    };
  }, [searchParams]);
  const [remoteDraft, setRemoteDraft] = useState<RemoteTaskDraft | null>(initialRemoteDraft);

  const [phase, setPhase] = useState<Phase>(initialTaskId || remoteDraft ? "converting" : "idle");
  const [transcript, setTranscript] = useState("");
  const [displayedTranscript, setDisplayedTranscript] = useState("");
  const [polished, setPolished] = useState("");
  const [summary, setSummary] = useState("");
  const [taskId, setTaskId] = useState(initialTaskId);
  const [error, setError] = useState("");
  const [qaInput, setQaInput] = useState("");
  const [qaLoading, setQaLoading] = useState(false);
  const [qaItems, setQaItems] = useState<QaItem[]>([]);
  const [showReader, setShowReader] = useState(false);
  const [readerText, setReaderText] = useState("");
  const [readerTitle, setReaderTitle] = useState("");
  const [summaryRawMode, setSummaryRawMode] = useState(false);
  const [historySaving, setHistorySaving] = useState(false);
  const [collapsed, setCollapsed] = useState<Record<string, boolean>>({});
  const [collapsedQaItems, setCollapsedQaItems] = useState<Record<string, boolean>>({});
  const [remoteStarting, setRemoteStarting] = useState(false);
  const [remoteMeta, setRemoteMeta] = useState<PostMetadata | null>(null);
  const [remoteFileName, setRemoteFileName] = useState("");
  const [downloadProgress, setDownloadProgress] = useState<TransferProgress>({ progress: 0, phase: "pending" });
  const [intakeProgress, setIntakeProgress] = useState<TransferProgress>({ progress: 0, phase: "pending" });
  const [downloaderUrl, setDownloaderUrl] = useState("");
  const [downloaderLoading, setDownloaderLoading] = useState(false);
  const [downloaderError, setDownloaderError] = useState("");
  const [downloaderResult, setDownloaderResult] = useState<DownloaderParseData | null>(null);
  const [downloaderCopiedValue, setDownloaderCopiedValue] = useState("");
  const [downloaderSendingToParse, setDownloaderSendingToParse] = useState(false);
  const [downloaderSendToParseMessage, setDownloaderSendToParseMessage] = useState("");

  const currentStep =
    phase === "idle"
      ? 0
      : phase === "converting"
        ? 1
        : phase === "transcribing"
          ? 2
          : phase === "polishing"
            ? 3
            : 4;

  const resetProcessingState = useCallback((nextPhase: Phase) => {
    setPhase(nextPhase);
    setTranscript("");
    setDisplayedTranscript("");
    transcriptTargetRef.current = "";
    transcriptSeqRef.current = 0;
    if (transcriptFrameRef.current) {
      cancelAnimationFrame(transcriptFrameRef.current);
      transcriptFrameRef.current = null;
    }
    transcriptLastFrameAtRef.current = 0;
    transcriptTypingCarryRef.current = 0;
    setPolished("");
    setSummary("");
    setError("");
    setQaItems([]);
    setCollapsed({});
    setCollapsedQaItems({});
    setSummaryRawMode(false);
    setDownloadProgress({ progress: 0, phase: "pending" });
    setIntakeProgress({ progress: 0, phase: "pending" });
  }, []);

  const startTranscriptTypewriter = useCallback(() => {
    if (transcriptFrameRef.current) return;

    const tick = (timestamp: number) => {
      setDisplayedTranscript((current) => {
        const target = transcriptTargetRef.current;
        if (current === target) {
          transcriptFrameRef.current = null;
          transcriptLastFrameAtRef.current = 0;
          transcriptTypingCarryRef.current = 0;
          return current;
        }
        if (!target.startsWith(current)) {
          transcriptLastFrameAtRef.current = 0;
          transcriptTypingCarryRef.current = 0;
          return target;
        }
        const remaining = target.length - current.length;
        const elapsedMs = transcriptLastFrameAtRef.current
          ? Math.min(120, Math.max(8, timestamp - transcriptLastFrameAtRef.current))
          : 16;
        transcriptLastFrameAtRef.current = timestamp;
        const charsPerSecond = Math.max(48, Math.min(140, remaining / 3.8));
        transcriptTypingCarryRef.current += (charsPerSecond * elapsedMs) / 1000;
        const step = Math.max(1, Math.min(remaining, Math.floor(transcriptTypingCarryRef.current)));
        transcriptTypingCarryRef.current = Math.max(0, transcriptTypingCarryRef.current - step);
        return target.slice(0, current.length + step);
      });

      if (transcriptFrameRef.current !== null) {
        transcriptFrameRef.current = requestAnimationFrame(tick);
      }
    };

    transcriptFrameRef.current = requestAnimationFrame(tick);
  }, []);

  const animateTranscriptTo = useCallback((nextText: string) => {
    const currentTarget = transcriptTargetRef.current;
    if (!nextText && currentTarget) return;
    if (nextText.length < currentTarget.length && currentTarget.startsWith(nextText)) return;

    setTranscript((current) => {
      if (nextText.length < current.length && current.startsWith(nextText)) return current;
      return nextText;
    });
    transcriptTargetRef.current = nextText;
    startTranscriptTypewriter();
  }, [startTranscriptTypewriter]);

  const appendTranscriptDelta = useCallback((delta: string, fullText?: string, seq?: number) => {
    if (!delta && !fullText) return;
    if (seq != null && seq <= transcriptSeqRef.current) return;
    if (seq != null) transcriptSeqRef.current = seq;

    const nextFromFull = fullText || "";
    setTranscript((current) => {
      if (nextFromFull) {
        if (nextFromFull.length < current.length && current.startsWith(nextFromFull)) return current;
        transcriptTargetRef.current = nextFromFull;
        return nextFromFull;
      }
      const next = `${current}${delta}`;
      transcriptTargetRef.current = next;
      return next;
    });
    startTranscriptTypewriter();
  }, [startTranscriptTypewriter]);

  const applyTaskSnapshot = useCallback((snapshot: Awaited<ReturnType<typeof fetchTaskSnapshot>>) => {
    const status = snapshot.status as Phase;
    if (snapshot.transcript || !transcriptTargetRef.current) {
      animateTranscriptTo(snapshot.transcript || "");
    }
    setPolished(snapshot.polished || "");
    setSummary(snapshot.summary || "");
    setRemoteFileName(snapshot.file_name || "");
    setDownloadProgress((current) => {
      if (shouldKeepCompletedProgress(current, snapshot.download_progress)) {
        return current;
      }
      if (hasTransferProgressSignal(snapshot.download_progress)) {
        return snapshot.download_progress;
      }
      return current.progress === 100 ? current : { progress: 0, phase: "pending" };
    });
    setIntakeProgress((current) => {
      if (shouldKeepCompletedProgress(current, snapshot.intake_progress)) {
        return current;
      }
      if (hasTransferProgressSignal(snapshot.intake_progress)) {
        return snapshot.intake_progress;
      }
      return current.progress === 100 ? current : { progress: 0, phase: "pending" };
    });

    if (snapshot.metadata) {
      setRemoteMeta(snapshot.metadata as unknown as PostMetadata);
    }

    if (snapshot.error) {
      setError(snapshot.error);
    }

    if (["downloading", "converting"].includes(String(snapshot.status))) {
      setPhase("converting");
      return;
    }

    if (["transcribing", "polishing", "summarizing", "done", "error"].includes(String(snapshot.status))) {
      setPhase(status);
    }
  }, [animateTranscriptTo]);

  const refreshTaskSnapshot = useCallback(async (id: string) => {
    try {
      const snapshot = await fetchTaskSnapshot(id);
      applyTaskSnapshot(snapshot);
    } catch {
      return;
    }
  }, [applyTaskSnapshot]);

  const downloadRemoteFileInBrowser = useCallback(async (draft: RemoteTaskDraft): Promise<File> => {
    const response = await fetch(draft.downloadUrl, { cache: "no-store" });
    if (!response.ok || !response.body) {
      throw new Error("浏览器下载远程文件失败");
    }

    const totalBytes = Number(response.headers.get("Content-Length") || 0);
    const reader = response.body.getReader();
    const chunks: Uint8Array[] = [];
    let downloadedBytes = 0;
    const startedAt = Date.now();

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      if (!value) continue;

      chunks.push(value);
      downloadedBytes += value.byteLength;
      const elapsed = Math.max((Date.now() - startedAt) / 1000, 0.001);
      const speed = downloadedBytes / elapsed;
      const progress = totalBytes ? Math.round((downloadedBytes / totalBytes) * 100) : Math.min(95, Math.round(downloadedBytes / (4 * 1024 * 1024)));

      setDownloadProgress({
        phase: "browser-downloading",
        progress,
        downloaded_bytes: downloadedBytes,
        total_bytes: totalBytes,
        eta_seconds: totalBytes && speed > 0 ? Math.round((totalBytes - downloadedBytes) / speed) : null,
        speed_bytes_per_sec: Math.round(speed),
      });
    }

    const blob = new Blob(chunks.map((chunk) => chunk.slice().buffer), { type: response.headers.get("Content-Type") || "video/mp4" });
    const safeTitle = draft.title.replace(/[\\/:*?"<>|]/g, "_").trim() || "remote-video";
    const fileName = /\.[a-z0-9]{2,6}$/i.test(safeTitle) ? safeTitle : `${safeTitle}.mp4`;

    setDownloadProgress({
      phase: "done",
      progress: 100,
      downloaded_bytes: blob.size,
      total_bytes: totalBytes || blob.size,
      eta_seconds: 0,
      speed_bytes_per_sec: 0,
    });

    return new File([blob], fileName, { type: blob.type || "video/mp4" });
  }, []);

  const startTaskMonitoring = useCallback((id: string) => {
    setTaskId(id);
    void refreshTaskSnapshot(id);

    if (esRef.current) {
      esRef.current.close();
      esRef.current = null;
    }

    const es = createSSEConnection(id, {
      onEvent: (event: SSEEvent) => {
        switch (event.type) {
          case "download_start":
            setRemoteFileName(event.file_name || "");
            setRemoteMeta(event.metadata || null);
            setDownloadProgress({
              progress: event.progress || 0,
              downloaded_bytes: event.downloaded_bytes || 0,
              total_bytes: event.total_bytes || 0,
              eta_seconds: event.eta_seconds,
              speed_bytes_per_sec: event.speed_bytes_per_sec || 0,
              phase: event.phase || "running",
            });
            setPhase("converting");
            break;
          case "download_progress":
            setDownloadProgress({
              progress: event.progress || 0,
              downloaded_bytes: event.downloaded_bytes || 0,
              total_bytes: event.total_bytes || 0,
              eta_seconds: event.eta_seconds,
              speed_bytes_per_sec: event.speed_bytes_per_sec || 0,
              phase: event.phase || "running",
            });
            setPhase("converting");
            break;
          case "ingest_progress":
            setIntakeProgress({
              progress: event.progress || 0,
              processed_bytes: event.processed_bytes || 0,
              total_bytes: event.total_bytes || 0,
              eta_seconds: event.eta_seconds,
              phase: event.phase || "preparing",
            });
            break;
          case "download_done":
            setDownloadProgress({
              progress: 100,
              downloaded_bytes: event.downloaded_bytes || 0,
              total_bytes: event.total_bytes || 0,
              eta_seconds: 0,
              speed_bytes_per_sec: event.speed_bytes_per_sec || 0,
              phase: "done",
            });
            break;
          case "ingest_done":
            setIntakeProgress({
              progress: event.progress || 100,
              processed_bytes: event.processed_bytes || 0,
              total_bytes: event.total_bytes || 0,
              eta_seconds: 0,
              phase: "done",
            });
            break;
          case "transcribe_start":
            setPhase("transcribing");
            break;
          case "chunk":
            if (event.delta) {
              appendTranscriptDelta(event.delta, event.text, event.seq);
            } else {
              animateTranscriptTo(event.text || "");
            }
            setPhase("transcribing");
            break;
          case "transcribe_done":
            setPhase("polishing");
            break;
          case "polish_start":
            setPhase("polishing");
            break;
          case "polish_char":
            setPolished(event.text || "");
            setPhase("polishing");
            break;
          case "polish_done":
            setPolished((prev) => event.polished_text || event.text || prev);
            setPhase("summarizing");
            break;
          case "summary_start":
            setPhase("summarizing");
            break;
          case "summary_char":
            setSummary(event.text || "");
            setPhase("summarizing");
            break;
          case "summary_done":
            setSummary((prev) => event.summary_text || event.text || prev);
            setPolished((prev) => event.polished_text || prev);
            break;
          case "done":
            if (event.raw_text) {
              animateTranscriptTo(event.raw_text);
            }
            setPolished((prev) => event.polished_text || prev);
            setSummary((prev) => event.summary_text || prev);
            setRemoteMeta(event.metadata || remoteMeta);
            setPhase("done");
            setSearchParams({ task: id });
            void refreshTaskSnapshot(id);
            break;
          case "error":
            setError(event.error || "Unknown error");
            setPhase("error");
            break;
        }
      },
      onError: () => {
        void refreshTaskSnapshot(id);
      },
      onDone: () => {
        setPhase("done");
      },
    });

    esRef.current = es;
  }, [animateTranscriptTo, remoteMeta, refreshTaskSnapshot, setSearchParams]);

  const startRemoteWorkflow = useCallback(async (draftOverride?: RemoteTaskDraft) => {
    const draft = draftOverride || remoteDraft;
    if (!draft) return;
    const remoteStartKey = `${draft.downloadUrl}|${draft.title}|${draft.sourceUrl}`;
    if (remoteStarting || remoteStartKeyRef.current === remoteStartKey) return;
    remoteStartKeyRef.current = remoteStartKey;
    setSearchParams({}, { replace: true });
    setRemoteStarting(true);
    resetProcessingState("converting");
    setRemoteMeta({
      title: draft.title,
      categories: [draft.platform],
      tags: [draft.noteType || "video"],
    });

    try {
      setDownloadProgress({
        phase: "queued",
        progress: 0,
        downloaded_bytes: 0,
        total_bytes: 0,
        eta_seconds: null,
        speed_bytes_per_sec: 0,
      });
      const workflow = await sendDownloaderResultToParse({
        downloadUrl: draft.downloadUrl,
        title: draft.title,
        sourceUrl: draft.sourceUrl,
        metadata: {
          title: draft.title,
          platform: draft.platform,
          duration: draft.duration,
          noteType: draft.noteType,
          download_audio_url: draft.audioUrl,
          original_url: draft.sourceUrl,
        },
      });

      setRemoteFileName(workflow.file_name);
      startTaskMonitoring(workflow.task_id);
    } catch {
      try {
        const file = await downloadRemoteFileInBrowser(draft);
        setRemoteFileName(file.name);
        const id = await uploadFileWithProgress(file, (progress) => {
          setIntakeProgress(progress);
        });
        startTaskMonitoring(id);
      } catch (fallbackError) {
        setError(fallbackError instanceof Error ? fallbackError.message : String(fallbackError));
        setPhase("error");
      }
    } finally {
      setRemoteStarting(false);
    }
  }, [downloadRemoteFileInBrowser, remoteDraft, remoteStarting, resetProcessingState, setSearchParams, startTaskMonitoring]);

  const toggleCollapse = useCallback((name: string) => {
    setCollapsed((prev) => ({ ...prev, [name]: !prev[name] }));
  }, []);

  const toggleQaItemCollapse = useCallback((id: string) => {
    setCollapsedQaItems((prev) => ({ ...prev, [id]: !prev[id] }));
  }, []);

  const handleFileSelected = useCallback(async (file: File) => {
    setSearchParams({});
    setRemoteDraft(null);
    remoteStartKeyRef.current = "";
    resetProcessingState("converting");
    setTaskId("");
    setRemoteMeta(null);
    setRemoteFileName("");

    try {
      const id = await uploadFile(file);
      startTaskMonitoring(id);
    } catch (e) {
      setError(e instanceof Error ? e.message : String(e));
      setPhase("error");
    }
  }, [resetProcessingState, setSearchParams, startTaskMonitoring]);

  const handleRetryRemote = useCallback(() => {
    if (!remoteDraft) return;
    remoteStartKeyRef.current = "";
    startRemoteWorkflow();
  }, [remoteDraft, startRemoteWorkflow]);

  const handleFileChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0];
      if (file) handleFileSelected(file);
    },
    [handleFileSelected]
  );

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault();
      const file = e.dataTransfer.files[0];
      if (file) handleFileSelected(file);
    },
    [handleFileSelected]
  );

  const handleExport = useCallback(async () => {
    if (!taskId || phase !== "done") return;
    try {
      await exportMarkdown(taskId);
    } catch {
      setError("Export failed");
    }
  }, [taskId, phase]);

  const handleSaveHistory = useCallback(async () => {
    if (!taskId || phase !== "done" || historySaving) return;
    setHistorySaving(true);
    try {
      const record = await saveMP4History(taskId);
      notification.success({ title: "已保存历史记录", description: record.title })
    } catch (e) {
      const msg = e instanceof Error ? e.message : "保存历史记录失败"
      setError(msg)
      notification.danger({ title: "保存历史记录失败", description: msg })
    } finally {
      setHistorySaving(false);
    }
  }, [historySaving, phase, taskId]);

  const handleQASubmit = useCallback(async (prefilledQuestion?: string) => {
    const question = (prefilledQuestion ?? qaInput).trim();
    if (!taskId || !question || qaLoading) return;

    const tempId = `qa-item-${Date.now()}`;
    setQaLoading(true);
    setQaInput("");
    setCollapsed((prev) => ({ ...prev, ask: false }));
    setCollapsedQaItems((prev) => ({ ...prev, [tempId]: false }));
    setQaItems((prev) => [{ id: tempId, question, loading: true }, ...prev]);
    notification.info({ title: "Ask AI 已发送", description: question })

    try {
      const answer = await askQuestion(taskId, question);
      const resolvedItem: QaItem = {
        id: tempId,
        question,
        answerHtml: renderQaAnswer(answer, question),
      };
      setQaItems((prev) => prev.map((item) => (item.id === tempId ? resolvedItem : item)));
    } catch {
      setQaItems((prev) => prev.filter((item) => item.id !== tempId));
      setError("Q&A request failed")
      notification.danger({ title: "Ask AI 失败", description: "Q&A request failed" })
    } finally {
      setQaLoading(false);
    }
  }, [taskId, qaInput, qaLoading]);

  const handleQAKeyDown = useCallback(
    (e: React.KeyboardEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      if (e.key === "Enter" && !e.shiftKey) {
        e.preventDefault();
        handleQASubmit();
      }
    },
    [handleQASubmit]
  );

  const handleQAChipClick = useCallback((prompt: string) => {
    void handleQASubmit(prompt);
  }, [handleQASubmit]);

  const handleCopyText = useCallback((text: string) => {
    navigator.clipboard.writeText(text);
  }, []);

  const handleDownloaderCopy = useCallback(async (value: string) => {
    try {
      await navigator.clipboard.writeText(value);
      setDownloaderCopiedValue(value);
      window.setTimeout(() => {
        setDownloaderCopiedValue((current) => (current === value ? "" : current));
      }, 1500);
    } catch {
      setDownloaderError("复制失败，请手动复制链接");
    }
  }, []);

  const handleDownloaderPaste = useCallback(async () => {
    try {
      const text = await navigator.clipboard.readText();
      setDownloaderUrl(text);
      setDownloaderError("");
    } catch {
      setDownloaderError("无法读取剪贴板，请手动粘贴链接");
    }
  }, []);

  const handleDownloaderSubmit = useCallback(async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const trimmed = downloaderUrl.trim();
    if (!trimmed) {
      setDownloaderError("请输入要解析的链接");
      return;
    }

    setDownloaderLoading(true);
    setDownloaderError("");
    setDownloaderResult(null);
    setDownloaderSendToParseMessage("");

    try {
      setDownloaderResult(await parseDownloaderUrl(trimmed));
    } catch (err) {
      setDownloaderError(err instanceof Error ? err.message : "解析失败");
    } finally {
      setDownloaderLoading(false);
    }
  }, [downloaderUrl]);

  const handleDownloaderSendToParse = useCallback(() => {
    const result = downloaderResult;
    if (!result || downloaderSendingToParse) return;
    const mediaUrl = selectDownloaderMediaUrl(result);
    if (!mediaUrl || downloaderSendingToParse) return;

    setDownloaderSendingToParse(true);
    setDownloaderSendToParseMessage("");
    setDownloaderError("");

    try {
      const payload: RemoteParsePayload = {
        downloadUrl: mediaUrl,
        title: result.title,
        sourceUrl: result.url,
        metadata: {
          title: result.title,
          platform: result.platform,
          duration: result.duration,
          noteType: result.noteType,
          download_audio_url: result.originDownloadAudioUrl || result.downloadAudioUrl,
          original_url: result.url,
        },
      };

      setDownloaderSendToParseMessage("正在接入 MP4 to Text 工作流...");
      setTaskId("");
      remoteStartKeyRef.current = "";
      const nextDraft: RemoteTaskDraft = {
        downloadUrl: payload.downloadUrl,
        title: payload.title || "未命名视频",
        sourceUrl: payload.sourceUrl || payload.downloadUrl,
        platform: result.platform || "Downloader",
        duration: result.duration,
        noteType: result.noteType || "video",
        audioUrl: result.originDownloadAudioUrl || result.downloadAudioUrl || "",
      };
      setRemoteDraft(nextDraft);
      setSearchParams({}, { replace: true });
      void startRemoteWorkflow(nextDraft);
    } catch (err) {
      setDownloaderError(err instanceof Error ? err.message : "Send to parse 失败");
      setDownloaderSendingToParse(false);
    }
  }, [downloaderResult, downloaderSendingToParse, setSearchParams, startRemoteWorkflow]);

  const handleDownloaderClear = useCallback(() => {
    setDownloaderUrl("");
    setDownloaderError("");
    setDownloaderResult(null);
    setRemoteDraft(null);
    setDownloaderCopiedValue("");
    setDownloaderSendingToParse(false);
    setDownloaderSendToParseMessage("");
  }, []);

  const handleOpenReader = useCallback((text: string, title: string) => {
    setReaderText(text);
    setReaderTitle(title);
    setShowReader(true);
  }, []);

  const handleDocumentClick = useCallback(
    (e: MouseEvent) => {
      const target = e.target as HTMLElement;

      const chip = target.closest(".qa-followup-chip") as HTMLButtonElement | null;
      if (chip) {
        const prompt = chip.dataset.followup;
        if (prompt) handleQAChipClick(prompt);
        return;
      }

      const readBtn = target.closest(".read-full-qa") as HTMLButtonElement | null;
      if (readBtn) {
        const text = readBtn.dataset.text || "";
        const title = readBtn.dataset.title || "Ask AI Answer";
        handleOpenReader(text, title);
        return;
      }

      const copyBtn = target.closest(".copy-full-qa") as HTMLButtonElement | null;
      if (copyBtn) {
        const text = copyBtn.dataset.text || "";
        handleCopyText(text);
      }
    },
    [handleQAChipClick, handleOpenReader, handleCopyText]
  );

  useEffect(() => {
    document.addEventListener("click", handleDocumentClick);
    return () => document.removeEventListener("click", handleDocumentClick);
  }, [handleDocumentClick]);

  useEffect(() => {
    if (initialTaskId && !esRef.current) {
      const timer = window.setTimeout(() => {
        startTaskMonitoring(initialTaskId);
      }, 0);
      return () => window.clearTimeout(timer);
    }

    if (!initialTaskId && remoteDraft && !taskId && !remoteStarting) {
      const timer = window.setTimeout(() => {
        startRemoteWorkflow();
      }, 0);
      return () => window.clearTimeout(timer);
    }
  }, [initialTaskId, remoteDraft, remoteStarting, startRemoteWorkflow, startTaskMonitoring, taskId]);

  useEffect(() => {
    if (!remoteDraft) return;
    setDownloaderSendingToParse(false);
  }, [remoteDraft]);

  useEffect(() => {
    if (!taskId || phase === "done" || phase === "error") return;

    const timer = window.setInterval(() => {
      void refreshTaskSnapshot(taskId);
    }, 1000);

    return () => window.clearInterval(timer);
  }, [phase, refreshTaskSnapshot, taskId]);

  useEffect(() => {
    return () => {
      if (esRef.current) {
        esRef.current.close();
      }
      if (transcriptFrameRef.current) {
        cancelAnimationFrame(transcriptFrameRef.current);
      }
      transcriptLastFrameAtRef.current = 0;
      transcriptTypingCarryRef.current = 0;
    };
  }, []);

  const transcriptHtml = `<pre>${displayedTranscript}</pre>`;
  const polishedHtml = `<pre>${polished}</pre>`;
  const summaryHtml = buildSummaryCards(summary);

  const showResults =
    phase === "done" ||
    phase === "polishing" ||
    phase === "summarizing" ||
    phase === "transcribing";
  const showProcessingPanel = phase === "converting" || phase === "transcribing";
  const activeAlert = (() => {
    if (error) {
      return {
        variant: "destructive" as const,
        icon: <AlertCircle className="size-4" />,
        title: "Workflow needs attention",
        description: error,
      };
    }

    if (phase === "done") {
      return {
        variant: "default" as const,
        icon: <CheckCircle2 className="size-4" />,
        title: "Workflow completed",
        description: "Transcript, polish and summary are ready. You can copy, read or export the result.",
      };
    }

    if (remoteDraft && phase === "converting") {
      return {
        variant: "default" as const,
        icon: <Info className="size-4" />,
        title: "Remote resource is being prepared",
        description: "The page is downloading the parsed video first, then uploading it to MP4 to Word automatically.",
      };
    }

    if (phase === "transcribing") {
      return {
        variant: "default" as const,
        icon: <Info className="size-4" />,
        title: "Transcription is streaming",
        description: "Transcript will render progressively while the backend keeps processing the media.",
      };
    }

    if (phase === "polishing" || phase === "summarizing") {
      return {
        variant: "default" as const,
        icon: <Info className="size-4" />,
        title: phase === "polishing" ? "AI polish is running" : "AI summary is running",
        description: "The transcript has been captured and AI post-processing is now updating the result panels.",
      };
    }

    return null;
  })();

  const processProgress = (() => {
    if (phase === "idle") return 0;
    if (phase === "converting") {
      const downloadPart = remoteDraft ? Math.min(downloadProgress.progress || 0, 100) * 0.22 : 12;
      const ingestPart = Math.min(intakeProgress.progress || 0, 100) * 0.18;
      return Math.round(Math.max(10, Math.min(38, downloadPart + ingestPart + 8)));
    }
    if (phase === "transcribing") return transcript ? 58 : 45;
    if (phase === "polishing") return 74;
    if (phase === "summarizing") return 88;
    if (phase === "done") return 100;
    return 100;
  })();

  const workflowSteps = [
    { label: "Upload", description: "Input source", icon: UploadCloud },
    { label: "Convert", description: "Audio ready", icon: FileAudio },
    { label: "Transcribe", description: "Text stream", icon: Captions },
    { label: "Polish", description: "AI rewrite", icon: Sparkles },
    { label: "Summary", description: "Final notes", icon: ListChecks },
  ];
  const phaseLabel = phase === "idle" ? "Idle" : phase === "done" ? "Done" : phase === "error" ? "Error" : phase;

  return (
    <WorkspaceShell sectionLabel="MP4 to Word" pageTitle="Workspace">
      <div className="flex w-full flex-col gap-5">
        <style>{QA_STYLE_FIX}</style>

        <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div className="min-w-0 flex-1 space-y-2">
            <div className="flex items-center gap-2 text-sm font-medium text-muted-foreground">
              <FileAudio className="size-4" />
              MP4 to Word
            </div>
            <h1 className="text-2xl font-semibold text-foreground">MP4 to Text</h1>
            <p className="max-w-3xl text-sm leading-6 text-muted-foreground">
              上传本地音视频或接收 Downloader 解析结果，完成转写、AI polish、summary、问答和历史归档。
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="outline" onClick={() => fileInputRef.current?.click()}>
              <UploadCloud className="size-4" />
              Upload
            </Button>
            <Button asChild variant="secondary">
              <Link to="/mp4-to-word/history">
                <History className="size-4" />
                History
              </Link>
            </Button>
          </div>
        </div>

        <section className="rounded-md bg-[#f9f9f9] px-3 pb-4 pt-3">
          {!downloaderResult && !downloaderLoading ? (
            <div>
              <div className="mb-2 flex items-center justify-between gap-3">
                <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
                  <LinkIcon className="size-4" />
                  Downloader
                </div>
                <Badge variant="outline">remote intake</Badge>
              </div>
              <form onSubmit={handleDownloaderSubmit} className="grid gap-2 lg:grid-cols-[minmax(0,1fr)_120px_140px]">
                <input
                  value={downloaderUrl}
                  onChange={(event) => setDownloaderUrl(event.target.value)}
                  placeholder="粘贴视频 / 帖子链接，例如 Bilibili、抖音、小红书等"
                  required
                  className="h-9 w-full rounded-md border-0 bg-muted px-3 text-sm outline-none transition placeholder:text-muted-foreground focus:bg-muted/80"
                />
                <Button type="button" variant="secondary" onClick={handleDownloaderPaste}>
                  粘贴链接
                </Button>
                <Button type="submit" variant="secondary" disabled={!downloaderUrl.trim()}>
                  <LinkIcon className="size-4" />
                  解析链接
                </Button>
              </form>
              {downloaderError ? <div className="mt-3 rounded-md bg-destructive/10 px-3 py-2 text-sm text-destructive">{downloaderError}</div> : null}
            </div>
          ) : downloaderLoading ? (
            <div className="p-1">
              <LoadingState />
            </div>
          ) : downloaderResult ? (
            <div className="space-y-3">
              <div className="grid gap-3 rounded-md bg-muted/45 p-3 xl:grid-cols-[minmax(0,1fr)_auto] xl:items-start">
                <div className="min-w-0">
                  <div className="flex flex-wrap gap-1.5">
                    <Badge variant="secondary">{downloaderResult.platform}</Badge>
                    <Badge variant="outline">{downloaderResult.noteType || "video"}</Badge>
                    {downloaderResult.duration ? <Badge variant="outline">{formatDuration(downloaderResult.duration)}</Badge> : null}
                  </div>
                  <div className="mt-2 line-clamp-2 text-sm font-semibold leading-6 text-foreground" title={downloaderResult.title}>
                    {downloaderResult.title}
                  </div>
                  <p className="mt-1 line-clamp-2 text-sm leading-6 text-muted-foreground">
                    {downloaderResult.desc || "暂无描述"}
                  </p>
                </div>
                <div className="flex shrink-0 flex-wrap gap-2">
                  <Button variant="secondary" onClick={handleDownloaderClear}>
                    <X className="size-4" />
                    Clear
                  </Button>
                  <Button variant="secondary" onClick={handleDownloaderSendToParse} disabled={!selectDownloaderMediaUrl(downloaderResult) || downloaderSendingToParse}>
                    {downloaderSendingToParse ? <Loader2 className="size-4 animate-spin" /> : <Send className="size-4" />}
                    {downloaderSendingToParse ? "Sending..." : "Send to Parse"}
                  </Button>
                </div>
              </div>

              <div className="grid gap-2">
                <LinkActionRow
                  label="Video Download"
                  value={downloaderResult.downloadVideoUrl}
                  copied={downloaderCopiedValue === downloaderResult.downloadVideoUrl}
                  onCopy={handleDownloaderCopy}
                />
                <LinkActionRow
                  label="Audio Download"
                  value={downloaderResult.downloadAudioUrl}
                  copied={downloaderCopiedValue === downloaderResult.downloadAudioUrl}
                  onCopy={handleDownloaderCopy}
                />
              </div>

              {downloaderSendToParseMessage ? (
                <div className="rounded-md bg-muted/50 px-3 py-2 text-sm text-muted-foreground">
                  {downloaderSendToParseMessage}
                </div>
              ) : null}

              {(downloaderResult.pages?.length || downloaderResult.videos?.length) ? (
                <div className="grid gap-3 xl:grid-cols-2">
                  {downloaderResult.pages?.length ? (
                    <div className="space-y-2">
                      <div className="text-sm font-medium text-foreground">Multi-part Items</div>
                      <div className="max-h-72 space-y-2 overflow-y-auto pr-1 [scrollbar-width:thin]">
                        {downloaderResult.pages.map((page) => (
                          <div key={`${page.cid}-${page.page}`} className="rounded-md bg-muted/60 p-3">
                            <div className="mb-2 flex items-center justify-between gap-4 text-sm">
                              <span className="min-w-0 truncate font-medium text-foreground">P{page.page} · {page.part}</span>
                              <span className="shrink-0 text-xs text-muted-foreground">{formatDuration(page.duration)}</span>
                            </div>
                            <div className="grid gap-2">
                              <LinkActionRow label="Video" value={page.downloadVideoUrl} copied={downloaderCopiedValue === page.downloadVideoUrl} onCopy={handleDownloaderCopy} />
                              <LinkActionRow label="Audio" value={page.downloadAudioUrl} copied={downloaderCopiedValue === page.downloadAudioUrl} onCopy={handleDownloaderCopy} />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : null}

                  {downloaderResult.videos?.length ? (
                    <div className="space-y-2">
                      <div className="text-sm font-medium text-foreground">Embedded Videos</div>
                      <div className="max-h-72 space-y-2 overflow-y-auto pr-1 [scrollbar-width:thin]">
                        {downloaderResult.videos.map((video) => (
                          <div key={video.id} className="rounded-md bg-muted/60 p-3">
                            <div className="mb-2 flex items-center gap-2 text-sm font-medium text-foreground">
                              <Video className="size-4" />
                              <span className="min-w-0 truncate">{video.title}</span>
                            </div>
                            <div className="grid gap-2">
                              <LinkActionRow label="Video" value={video.downloadVideoUrl} copied={downloaderCopiedValue === video.downloadVideoUrl} onCopy={handleDownloaderCopy} />
                              <LinkActionRow label="Audio" value={video.downloadAudioUrl} copied={downloaderCopiedValue === video.downloadAudioUrl} onCopy={handleDownloaderCopy} />
                            </div>
                          </div>
                        ))}
                      </div>
                    </div>
                  ) : null}
                </div>
              ) : null}
            </div>
          ) : null}
        </section>

        {activeAlert && (
          <Alert variant={activeAlert.variant} className="rounded-md border bg-background">
            {activeAlert.icon}
            <AlertTitle>{activeAlert.title}</AlertTitle>
            <AlertDescription>{activeAlert.description}</AlertDescription>
          </Alert>
        )}

        <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-4">
          <MetricTile
            icon={phase === "done" ? <CheckCircle2 className="size-4" /> : phase === "error" ? <AlertCircle className="size-4" /> : <RefreshCw className="size-4" />}
            label="Phase"
            value={phaseLabel}
            hint={taskId ? `task ${taskId}` : "no active task"}
          />
          <MetricTile
            icon={<RefreshCw className={`size-4 ${phase !== "idle" && phase !== "done" && phase !== "error" ? "animate-spin" : ""}`} />}
            label="Progress"
            value={`${processProgress}%`}
            hint={remoteDraft ? "remote intake" : "local workflow"}
          />
          <MetricTile
            icon={<Captions className="size-4" />}
            label="Transcript"
            value={transcript.length.toLocaleString()}
            hint="characters"
          />
          <MetricTile
            icon={<ListChecks className="size-4" />}
            label="Outputs"
            value={summary ? "ready" : polished ? "polish" : "pending"}
            hint={`${qaItems.length} Ask AI answers`}
          />
        </div>

        <div className="grid gap-4 xl:grid-cols-[minmax(0,1.45fr)_minmax(320px,0.55fr)]">
          <section className="overflow-hidden rounded-md bg-muted/50">
            <div className="flex items-center justify-between gap-3 border-b border-border/50 px-4 py-3">
              <div className="min-w-0">
                <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
                  <UploadCloud className="size-4" />
                  Intake
                </div>
                <div className="mt-1 text-xs text-muted-foreground">
                  本地文件上传或 Downloader 远程资源接管。
                </div>
              </div>
              <Badge variant={remoteDraft ? "default" : "secondary"}>{remoteDraft ? "remote" : "local"}</Badge>
            </div>

            <div className="grid gap-3 p-3 lg:grid-cols-[minmax(320px,0.55fr)_minmax(0,1fr)]">
              <div
                className="cursor-pointer rounded-md border border-dashed border-muted-foreground/45 bg-background p-4 transition hover:border-foreground/45 hover:bg-muted/35"
                onClick={() => fileInputRef.current?.click()}
                onDragOver={(e) => e.preventDefault()}
                onDrop={handleDrop}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="video/*,audio/*"
                  style={{ display: "none" }}
                  onChange={handleFileChange}
                />
                <div className="flex min-h-[166px] flex-col items-center justify-center text-center">
                  <UploadCloud className="size-8 text-muted-foreground" />
                  <div className="mt-3 text-xl font-semibold text-foreground">Click to upload</div>
                  <div className="mt-2 max-w-lg text-sm leading-5 text-muted-foreground">
                    支持 MP4、MP3、WAV、M4A；页面会保留实时转写流和 AI 后处理结果。
                  </div>
                </div>
              </div>

              <div className="rounded-md bg-background p-3">
                <div className="text-xs font-medium uppercase text-muted-foreground">Current source</div>
                {remoteDraft ? (
                  <div className="mt-3 space-y-3">
                    <div>
                      <div className="line-clamp-2 text-sm font-semibold text-foreground" title={remoteDraft.title}>
                        {remoteDraft.title}
                      </div>
                      <div className="mt-1 flex flex-wrap gap-1.5">
                        <Badge variant="secondary">{remoteDraft.platform}</Badge>
                        <Badge variant="outline">{remoteDraft.noteType || "video"}</Badge>
                      </div>
                    </div>
                    <div className="grid gap-2 text-xs">
                      <div className="rounded-md bg-muted/60 px-3 py-2">
                        <span className="text-muted-foreground">Duration</span>
                        <div className="mt-1 font-medium text-foreground">{formatMediaDuration(remoteDraft.duration)}</div>
                      </div>
                      <div className="rounded-md bg-muted/60 px-3 py-2">
                        <span className="text-muted-foreground">File</span>
                        <div className="mt-1 truncate font-medium text-foreground">{remoteFileName || "Resolving..."}</div>
                      </div>
                    </div>
                    <Separator />
                    <div className="flex items-center justify-between gap-2">
                      <div className="min-w-0 truncate text-xs text-muted-foreground" title={remoteDraft.sourceUrl}>
                        {compactUrl(remoteDraft.sourceUrl)}
                      </div>
                      <div className="flex gap-1">
                        <Button size="icon-sm" variant="ghost" title="Copy source" aria-label="Copy source" onClick={() => handleCopyText(remoteDraft.sourceUrl)}>
                          <Copy className="size-4" />
                        </Button>
                        <Button size="icon-sm" variant="ghost" title="Open source" aria-label="Open source" onClick={() => window.open(remoteDraft.sourceUrl, "_blank", "noopener,noreferrer")}>
                          <ExternalLink className="size-4" />
                        </Button>
                      </div>
                    </div>
                  </div>
                ) : (
                  <div className="mt-3 rounded-md bg-background/70 px-3 py-6 text-center text-sm text-muted-foreground">
                    暂无远程任务。可以直接上传本地文件开始。
                  </div>
                )}
              </div>
            </div>
          </section>

          <section className="overflow-hidden rounded-md bg-muted/50">
            <div className="flex items-center justify-between gap-3 border-b border-border/50 px-4 py-3">
              <div>
                <div className="flex items-center gap-2 text-sm font-semibold text-foreground">
                  <ListChecks className="size-4" />
                  Workflow
                </div>
                <div className="mt-1 text-xs text-muted-foreground">链路状态和进度。</div>
              </div>
              <div className="font-mono text-sm font-semibold text-foreground">{processProgress}%</div>
            </div>
            <div className="space-y-3 p-3">
              <Progress value={processProgress} className="h-2" />
              <div className="space-y-1.5">
                {workflowSteps.map((step, index) => {
                  const Icon = step.icon;
                  const active = index === currentStep;
                  const completed = index < currentStep || phase === "done";
                  return (
                    <div
                      key={step.label}
                      className={`flex items-center gap-3 rounded-md px-3 py-2 ${
                        active ? "bg-background text-foreground" : completed ? "bg-background/70 text-foreground" : "text-muted-foreground"
                      }`}
                    >
                      <span className={`inline-flex size-7 items-center justify-center rounded-md ${active ? "bg-foreground text-background" : "bg-muted"}`}>
                        {completed ? <CheckCircle2 className="size-4" /> : <Icon className="size-4" />}
                      </span>
                      <span className="min-w-0 flex-1">
                        <span className="block truncate text-sm font-medium">{step.label}</span>
                        <span className="block truncate text-xs text-muted-foreground">{step.description}</span>
                      </span>
                    </div>
                  );
                })}
              </div>
            </div>
          </section>
        </div>

        {showProcessingPanel && (
          <div className="mb-5 grid gap-5 lg:grid-cols-2">
            {remoteDraft ? (
              <ProgressCard
                title="Download"
                description={remoteStarting ? "正在创建远程任务并准备下载资源。" : "正在拉取 Downloader 已解析出的远程视频资源。"}
                progress={downloadProgress}
                transferred={downloadProgress.downloaded_bytes || 0}
                total={downloadProgress.total_bytes || 0}
                eta={downloadProgress.eta_seconds}
                speed={downloadProgress.speed_bytes_per_sec}
              />
            ) : null}
            <ProgressCard
              title={remoteDraft ? "Upload / Fallback Ingest" : "Processing"}
              description={remoteDraft ? "优先由浏览器下载后上传到 MP4 to Word；如果浏览器受 CORS 限制，会自动回退到后端接管。" : "本地文件已上传，正在转换音频并接入转写流程。"}
              progress={remoteDraft ? intakeProgress : { ...intakeProgress, progress: Math.max(intakeProgress.progress || 0, phase === "transcribing" ? 100 : 15) }}
              transferred={remoteDraft ? intakeProgress.processed_bytes || 0 : intakeProgress.processed_bytes || 0}
              total={remoteDraft ? intakeProgress.total_bytes || 0 : intakeProgress.total_bytes || 0}
              eta={remoteDraft ? intakeProgress.eta_seconds : intakeProgress.eta_seconds}
              speed={0}
            />
          </div>
        )}

        {phase === "error" && remoteDraft && (
          <div className="result-box" style={{ marginBottom: 20 }}>
            <div className="result-header">
              <span className="result-title">Workflow Failed</span>
            </div>
            <div className="result-body" style={{ minHeight: 0 }}>
              <p style={{ marginBottom: 16 }}>远程处理链路发生异常，你可以直接重试当前任务接管流程。</p>
              <button className="action-btn" onClick={handleRetryRemote} disabled={remoteStarting}>
                {remoteStarting ? "Retrying..." : "Retry Remote Flow"}
              </button>
            </div>
          </div>
        )}

        {showResults && (
          <div id="resultsArea" style={{ display: "block" }}>
            <div className="columns">
              <div className="result-box">
                <div className="result-header" onClick={() => toggleCollapse("transcript")}>
                  <span className="result-title">
                    <Captions className="size-4" /> Transcript
                  </span>
                  <div className="result-meta">
                    <Badge variant="secondary">{transcript.length} chars</Badge>
                    <Button
                      size="icon-sm"
                      variant="ghost"
                      title="Copy transcript"
                      aria-label="Copy transcript"
                      className="rounded-md"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleCopyText(transcript);
                      }}
                    >
                      <Copy className="size-4" />
                    </Button>
                  </div>
                </div>
                <div className={`result-body-wrap ${collapsed.transcript ? "collapsed" : "open"}`}>
                  <div className="result-body">
                    {displayedTranscript ? <div dangerouslySetInnerHTML={{ __html: transcriptHtml }} /> : <TextSkeletonBlock rows={10} />}
                  </div>
                </div>
              </div>

              <div className="result-box">
                <div className="result-header" onClick={() => toggleCollapse("polished")}>
                  <span className="result-title">
                    <Sparkles className="size-4" /> AI Polish
                  </span>
                  <div className="result-meta">
                    <Badge variant="secondary">{polished.length} chars</Badge>
                    <Button
                      size="icon-sm"
                      variant="ghost"
                      title="Read polish"
                      aria-label="Read polish"
                      className="rounded-md"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleOpenReader(polished, "AI Polish");
                      }}
                    >
                      <BookOpen className="size-4" />
                    </Button>
                    <Button
                      size="icon-sm"
                      variant="ghost"
                      title="Copy polish"
                      aria-label="Copy polish"
                      className="rounded-md"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleCopyText(polished);
                      }}
                    >
                      <Copy className="size-4" />
                    </Button>
                  </div>
                </div>
                <div className={`result-body-wrap ${collapsed.polished ? "collapsed" : "open"}`}>
                  <div className="result-body">
                    {polished ? <div dangerouslySetInnerHTML={{ __html: polishedHtml }} /> : <TextSkeletonBlock rows={10} />}
                  </div>
                </div>
              </div>
            </div>

            <div className="summary-box" style={{ marginBottom: 20 }}>
              <div className="result-header" onClick={() => toggleCollapse("summary")}>
                <span className="result-title">
                  <ListChecks className="size-4" /> AI Summary
                </span>
                <div className="result-meta">
                  <Button
                    size="icon-sm"
                    variant={summaryRawMode ? "default" : "ghost"}
                    title="Toggle raw summary"
                    aria-label="Toggle raw summary"
                    className="rounded-md"
                    onClick={(e) => {
                      e.stopPropagation();
                      setSummaryRawMode((prev) => !prev);
                    }}
                  >
                    <Code2 className="size-4" />
                  </Button>
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    title="Copy summary"
                    aria-label="Copy summary"
                    className="rounded-md"
                    onClick={(e) => {
                      e.stopPropagation();
                      handleCopyText(summary);
                    }}
                  >
                    <Copy className="size-4" />
                  </Button>
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    title="Export markdown"
                    aria-label="Export markdown"
                    className="rounded-md"
                    disabled={phase !== "done"}
                    onClick={(e) => {
                      e.stopPropagation();
                      void handleExport();
                    }}
                  >
                    <Download className="size-4" />
                  </Button>
                  <Button
                    size="icon-sm"
                    variant="ghost"
                    title="Export history"
                    aria-label="Export history"
                    className="rounded-md"
                    disabled={phase !== "done" || historySaving}
                    onClick={(e) => {
                      e.stopPropagation();
                      void handleSaveHistory();
                    }}
                  >
                    <FileText className="size-4" />
                  </Button>
                </div>
              </div>
              <div className={`result-body-wrap ${collapsed.summary ? "collapsed" : "open"}`}>
                {summary ? (
                  <div className={`summary-stage ${summaryRawMode ? "raw-mode" : ""}`}>
                    <div className="summary-workspace" dangerouslySetInnerHTML={{ __html: summaryHtml }} />
                    <div className="summary-raw-view">{summary}</div>
                    <div className="summary-raw">{summary}</div>
                  </div>
                ) : (
                  <div className="result-body">
                    <TextSkeletonBlock rows={8} />
                  </div>
                )}
              </div>
            </div>

            <AskSection
              qaItems={qaItems}
              collapsed={!!collapsed.ask}
              collapsedQaItems={collapsedQaItems}
              onToggleSection={() => toggleCollapse("ask")}
              onToggleItem={toggleQaItemCollapse}
              onFollowupClick={handleQAChipClick}
            />
          </div>
        )}

        <FloatingAskBar
          visible={showResults}
          qaInput={qaInput}
          qaLoading={qaLoading}
          taskId={taskId}
          onChange={setQaInput}
          onKeyDown={handleQAKeyDown as (e: React.KeyboardEvent<HTMLInputElement>) => void}
        />

        <ReaderModal show={showReader} title={readerTitle} text={readerText} onClose={() => setShowReader(false)} />
      </div>
    </WorkspaceShell>
  );
}

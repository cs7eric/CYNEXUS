import { ArrowLeft, FileQuestion } from "lucide-react"
import { Link } from "react-router-dom"

export default function MobileNotFoundPage() {
  return <div className="flex min-h-64 flex-col items-center justify-center gap-4 text-center">
    <FileQuestion className="size-8 text-muted-foreground" aria-hidden="true" />
    <h2 className="text-base font-semibold tracking-normal">Page not found</h2>
    <Link to="/m" replace className="mobile-app-link"><ArrowLeft className="size-4" aria-hidden="true" /><span className="text-sm">Back to Home</span></Link>
  </div>
}

import { Navigate, useParams, useSearchParams } from "react-router-dom"

/**
 * Compatibility route for bookmarks and older links. Position details now
 * render in the portfolio page's right-side drawer so the holdings context is
 * preserved while inspecting a symbol.
 */
export default function PersonalPortfolioStockDetailPage() {
  const { symbol = "" } = useParams()
  const [searchParams] = useSearchParams()
  const instrumentType = searchParams.get("instrumentType") === "ETF" ? "ETF" : "STOCK"
  return <Navigate to={`/personal-portfolio?stock=${encodeURIComponent(symbol)}&instrumentType=${instrumentType}`} replace />
}

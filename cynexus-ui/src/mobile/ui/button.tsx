import { Button, type ButtonProps } from "@heroui/react/button"

import { cn } from "@/lib/utils"

export function MobileButton({ className, variant = "secondary", ...props }: ButtonProps) {
  return <Button {...props} variant={variant} className={cn("min-h-11 rounded-lg", className)} />
}

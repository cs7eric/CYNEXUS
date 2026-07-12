declare module "prismjs" {
  const Prism: {
    languages: Record<string, unknown>;
    highlight: (...args: unknown[]) => string;
  };

  export default Prism;
}

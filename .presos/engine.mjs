import markdownItCodeSnippetEnhanced from '@gerhobbelt/markdown-it-code-snippet-enhanced'
import markdownItKroki from '@kazumatu981/markdown-it-kroki'

export default ({ marp }) => marp.use(markdownItCodeSnippetEnhanced).use(markdownItKroki)

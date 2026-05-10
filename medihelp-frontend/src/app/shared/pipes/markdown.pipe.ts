import { Pipe, PipeTransform } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';

/**
 * Renders the minimal subset of markdown the chatbot emits:
 *   **bold**, _italic_, `code`, headings (## / ###), pipe tables,
 *   unordered lists (- item / * item), line breaks.
 *
 * We don't pull a full library because the chatbot output is constrained
 * (no images, no links beyond emergency_url, no nested blocks).
 */
@Pipe({ name: 'markdown', standalone: true })
export class MarkdownPipe implements PipeTransform {
  constructor(private sanitizer: DomSanitizer) {}

  transform(value: string | null | undefined): SafeHtml {
    if (!value) return '';
    const html = this.toHtml(value);
    return this.sanitizer.bypassSecurityTrustHtml(html);
  }

  private toHtml(md: string): string {
    // 1. Escape HTML special chars so the LLM can't emit raw <script>.
    let s = md.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

    // 2. Pipe tables — must run before line-by-line conversions.
    s = this.renderTables(s);

    // 3. Headings (### before ## so longer match wins)
    s = s.replace(/^### (.+)$/gm, '<h3>$1</h3>');
    s = s.replace(/^## (.+)$/gm, '<h2>$1</h2>');

    // 4. Lists — group consecutive lines starting with "- " into <ul>
    s = s.replace(/(^|\n)((?:- .+\n?)+)/g, (_, lead, block) => {
      const items = block.trim().split(/\n/)
        .map((line: string) => `<li>${line.replace(/^- /, '')}</li>`)
        .join('');
      return `${lead}<ul>${items}</ul>`;
    });

    // 5. Inline formatting
    s = s.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    s = s.replace(/_(.+?)_/g, '<em>$1</em>');
    s = s.replace(/`(.+?)`/g, '<code>$1</code>');

    // 6. Remaining newlines → <br> (but not inside the HTML blocks we already made)
    s = s.replace(/(<\/(?:table|ul|h[1-6])>)\n+/g, '$1');
    s = s.replace(/\n/g, '<br>');

    return s;
  }

  private renderTables(s: string): string {
    return s.replace(/(?:^\|.+\|\n)+/gm, (block) => {
      const lines = block.trim().split(/\n/).filter(l => l.includes('|'));
      if (lines.length < 2) return block;
      const cells = lines.map(l =>
        l.split('|').map(c => c.trim()).filter((_, i, arr) => i > 0 && i < arr.length - 1)
      );
      const isSeparator = (row: string[]) => row.every(c => /^:?-+:?$/.test(c));
      const headerCells = cells[0];
      const bodyStart = isSeparator(cells[1] || []) ? 2 : 1;
      const head = `<thead><tr>${headerCells.map(h => `<th>${h}</th>`).join('')}</tr></thead>`;
      const body = `<tbody>${cells.slice(bodyStart).map(r =>
        `<tr>${r.map(c => `<td>${c}</td>`).join('')}</tr>`
      ).join('')}</tbody>`;
      return `<table class="md-table">${head}${body}</table>\n`;
    });
  }
}

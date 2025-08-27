import cx from 'classnames';

import { useProjectMode } from '../../../hooks';

export function SpreadsheetHighlight() {
  const { isAIPreviewMode, isAIPendingMode } = useProjectMode();

  if (!isAIPreviewMode && !isAIPendingMode) return null;

  return (
    <div
      className={cx(
        'absolute left-0 top-0 pointer-events-none size-full z-[999] ring-[3px] rounded ring-inset opacity-60',
        {
          'ring-strokeAccentSecondary': isAIPreviewMode,
          'ring-strokeAccentTertiary': isAIPendingMode,
        }
      )}
    ></div>
  );
}

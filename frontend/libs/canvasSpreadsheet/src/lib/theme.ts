import {
  AppTheme,
  convertThemeForCanvas,
  getHexColor,
  themeColors,
} from '@frontend/common';

import { FontColorName, FontFamilies } from './setup';
import { Theme } from './types';

function createSpreadsheetTheme(theme: AppTheme): Theme {
  const c = convertThemeForCanvas(themeColors[theme]);

  const getFontColorByTheme = (
    theme: AppTheme
  ): Record<string, FontColorName> => {
    if (theme === 'theme-dark') {
      return {
        textPrimary: FontColorName.darkTextPrimary,
        textSecondary: FontColorName.darkTextSecondary,
        textError: FontColorName.darkTextError,
        textAccent: FontColorName.darkTextAccent,
        textAccentSecondary: FontColorName.darkTextAccentSecondary,
      };
    }

    return {
      textPrimary: FontColorName.lightTextPrimary,
      textSecondary: FontColorName.lightTextSecondary,
      textError: FontColorName.lightTextError,
      textAccent: FontColorName.lightTextAccent,
      textAccentSecondary: FontColorName.lightTextAccentSecondary,
    };
  };
  const f = getFontColorByTheme(theme);

  return {
    themeName: theme,
    grid: {
      lineColor: theme === 'theme-dark' ? c.strokeGrid : c.strokeGridMain,
      bgColor: c.bgLayer1,
    },
    colNumber: {
      borderColor: c.strokeGridMain,
      bgColor: c.bgLayer2,
      bgColorSelected: c.bgLayer4,
      bgColorFullSelected: c.bgAccentPrimaryAlpha,
      bgColorHover: c.bgAccentPrimaryAlpha2,
      fontColorName: f.textSecondary,
      fontFamily: FontFamilies.InconsolataRegular,
      resizerHoverColor: c.strokeSecondary,
      resizerActiveColor: c.strokeAccentPrimary,
    },
    rowNumber: {
      bgColor: c.bgLayer2,
      bgColorSelected: c.bgLayer4,
      bgColorFullSelected: c.bgAccentPrimaryAlpha,
      bgColorHover: c.bgAccentPrimaryAlpha2,
      fontColorName: f.textSecondary,
      fontFamily: FontFamilies.InconsolataRegular,
    },
    scrollBar: {
      trackColor: c.bgLayer1,
      trackStrokeColor: c.strokeGridMain,
      thumbColor: c.bgLayer4,
      thumbColorHovered: c.textSecondary,
    },
    cell: {
      borderColor: c.strokeSecondary,
      bgColor: c.bgLayer1,
      bgEvenColor: c.bgGridColoredCell,
      tableHeaderBgColor: c.bgGridHeader,
      fieldHeaderBgColor: c.bgLayer4,
      totalBgColor: c.bgAccentTertiaryAlpha,
      cellFontColorName: f.textPrimary,
      cellFontFamily: FontFamilies.InconsolataRegular,
      boldCellFontColorName: f.textPrimary,
      boldCellFontFamily: FontFamilies.InconsolataBold,
      keyFontColorName: f.textError,
      keyFontFamily: FontFamilies.InconsolataBold,
      linkFontColorName: f.textAccent,
      linkFontFamily: FontFamilies.InconsolataRegular,
      indexFontColorName: f.textAccentSecondary,
      resizerHoverColor: c.strokeSecondary,
      resizerActiveColor: c.strokeAccentPrimary,
    },
    selection: {
      bgAlpha: 0.1,
      bgColor: c.strokeAccentPrimary,
      borderColor: c.strokeAccentPrimary,
    },
    override: {
      borderColor: c.strokeAccentSecondary,
    },
    error: {
      borderColor: c.strokeError,
    },
    pointClickSelection: {
      alpha: 1,
      color: c.strokeAccentSecondary,
      errorColor: c.strokeError,
      alignment: 0,
    },
    dottedSelection: {
      color: c.strokeAccentPrimary,
      alignment: 0,
      alpha: 1,
    },
    noteLabel: {
      bgColor: c.strokeAccentSecondary,
    },
    diff: {
      bgColor: c.bgAccentTertiary,
    },
    dndSelection: {
      borderColor: c.textWarning,
    },
    hiddenCell: {
      fontColorName: f.textSecondary,
      fontFamily: FontFamilies.InconsolataRegular,
    },
    tableShadow: {
      color: getHexColor(theme === 'theme-dark' ? 'ffffff' : '000000'),
      alpha: 0.6,
      rectangleAlpha: 0.2,
    },
  };
}

export function getTheme(theme: AppTheme): Theme {
  return createSpreadsheetTheme(
    theme === 'theme-dark' ? AppTheme.ThemeDark : AppTheme.ThemeLight
  );
}

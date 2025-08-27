import { Dropdown } from 'antd';
import { useCallback, useContext, useMemo } from 'react';

import Icon from '@ant-design/icons';
import { DotsIcon, getDropdownItem, MenuItem } from '@frontend/common';

import { ChatOverlayContext } from '../../../context';

export function AIPendingChangesContextMenu() {
  const { updateAIEditPendingChanges, isAIEditPendingChanges } =
    useContext(ChatOverlayContext);

  const items = useMemo(
    () => [
      getDropdownItem({
        key: 'editAll',
        label: isAIEditPendingChanges ? 'Cancel Edit All' : 'Allow Edit All',
      }),
    ],
    [isAIEditPendingChanges]
  );

  const onClick = useCallback(
    (item: MenuItem) => {
      switch (item?.key) {
        case 'editAll':
          updateAIEditPendingChanges(!isAIEditPendingChanges);
          break;
        default:
          break;
      }
    },
    [updateAIEditPendingChanges, isAIEditPendingChanges]
  );

  return (
    <Dropdown
      autoAdjustOverflow={true}
      destroyPopupOnHide={true}
      menu={{
        items: items,
        onClick,
      }}
      trigger={['click', 'contextMenu']}
    >
      <Icon
        className="w-[18px] text-textInverted hover:text-textPrimary"
        component={() => <DotsIcon />}
      />
    </Dropdown>
  );
}

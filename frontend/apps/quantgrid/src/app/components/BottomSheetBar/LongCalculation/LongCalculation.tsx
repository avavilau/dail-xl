import { Button, Popover } from 'antd';
import classNames from 'classnames';
import cx from 'classnames';
import { useCallback, useContext, useEffect, useMemo, useState } from 'react';

import Icon from '@ant-design/icons';
import {
  ClockCancelIcon,
  ClockExclamationIcon,
  primaryButtonClasses,
  primaryDisabledButtonClasses,
} from '@frontend/common';

import { LongCalcStatus } from '../../../common';
import { ProjectContext } from '../../../context';
import { useApiRequests } from '../../../hooks';
import { constructPath, encodeApiUrl } from '../../../utils';

export function LongCalculation() {
  const {
    longCalcStatus,
    setLongCalcStatus,
    projectPath,
    projectBucket,
    projectName,
    hasEditPermissions,
  } = useContext(ProjectContext);
  const { sendProjectCancel, sendProjectCalculate } = useApiRequests();
  const [isPopoverOpen, setIsPopoverOpen] = useState(false);

  const onAcceptLongCalc = useCallback(async () => {
    const res = await sendProjectCalculate({
      projectPath: encodeApiUrl(
        constructPath(['files', projectBucket, projectPath, projectName])
      ),
    });

    if (!res) return;

    setLongCalcStatus(LongCalcStatus.Accepted);
  }, [
    projectBucket,
    projectName,
    projectPath,
    sendProjectCalculate,
    setLongCalcStatus,
  ]);

  const onCancelLongCalc = useCallback(async () => {
    const res = await sendProjectCancel({
      projectPath: encodeApiUrl(
        constructPath(['files', projectBucket, projectPath, projectName])
      ),
    });

    if (!res) return;

    setLongCalcStatus(LongCalcStatus.Cancelled);
  }, [
    projectBucket,
    projectName,
    projectPath,
    sendProjectCancel,
    setLongCalcStatus,
  ]);

  const content = useMemo(() => {
    return (
      <div className="max-h-[50vh] max-w-[200px] overflow-auto thin-scrollbar flex flex-col gap-2">
        {longCalcStatus === LongCalcStatus.NeedAccept && (
          <>
            <span className="text-[13px] text-textPrimary">
              Running the project calculation may take a while.
            </span>
            <span className="text-[13px] text-textPrimary">
              Press <b>Accept</b> to keep calculations running in the
              background.
            </span>
            <Button
              className={classNames(
                primaryButtonClasses,
                primaryDisabledButtonClasses,
                'h-7'
              )}
              onClick={onAcceptLongCalc}
            >
              Accept long calculation
            </Button>
          </>
        )}

        {longCalcStatus === LongCalcStatus.Accepted && (
          <>
            <span className="text-[13px] text-textPrimary">
              A long calculation is in progress. You can cancel at any time.
            </span>
            <Button
              className={classNames(
                primaryButtonClasses,
                primaryDisabledButtonClasses,
                'h-7'
              )}
              onClick={onCancelLongCalc}
            >
              Cancel long calculation
            </Button>
          </>
        )}
      </div>
    );
  }, [longCalcStatus, onAcceptLongCalc, onCancelLongCalc]);

  const trigger = useMemo(() => {
    return (
      <div className="flex items-center gap-1">
        <Icon
          className={cx(
            'w-[18px]',
            longCalcStatus === LongCalcStatus.NeedAccept
              ? 'text-textError'
              : 'text-textWarning'
          )}
          component={() =>
            longCalcStatus === LongCalcStatus.NeedAccept ? (
              <ClockExclamationIcon />
            ) : (
              <ClockCancelIcon />
            )
          }
        />
      </div>
    );
  }, [longCalcStatus]);

  useEffect(() => {
    if (longCalcStatus !== LongCalcStatus.NeedAccept) return;

    setIsPopoverOpen(true);
  }, [longCalcStatus]);

  if (
    longCalcStatus === LongCalcStatus.None ||
    longCalcStatus === LongCalcStatus.Cancelled ||
    !hasEditPermissions
  )
    return null;

  return (
    <Popover
      content={content}
      destroyTooltipOnHide={true}
      open={isPopoverOpen}
      trigger={['hover', 'click']}
      onOpenChange={setIsPopoverOpen}
    >
      {trigger}
    </Popover>
  );
}

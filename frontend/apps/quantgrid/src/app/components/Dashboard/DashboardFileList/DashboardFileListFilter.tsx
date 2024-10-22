import cx from 'classnames';
import { DefaultOptionType } from 'rc-select/lib/Select';
import { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import Select, { components, SingleValue } from 'react-select';

import Icon from '@ant-design/icons';
import {
  CSVFileIcon,
  FileIcon,
  FilterIcon,
  FolderIcon,
  QGLogo,
  SelectClasses,
} from '@frontend/common';

import { DashboardFilter } from '../../../common';
import { DashboardContext } from '../../../context';

type FilterOptions = {
  label: string;
  value: DashboardFilter;
};

const filterOptions: FilterOptions[] = [
  {
    label: 'All types',
    value: 'all',
  },
  {
    label: 'Folders',
    value: 'folders',
  },
  {
    label: 'Projects',
    value: 'projects',
  },
  {
    label: 'Files',
    value: 'files',
  },
  {
    label: 'CSV files',
    value: 'csvFiles',
  },
];

type SelectIconProps = {
  size: number;
  isTransparent: boolean;
  filter: DashboardFilter;
};

function SelectIcon({ size, isTransparent, filter }: SelectIconProps) {
  const itemIcon = useMemo(() => {
    switch (filter) {
      case 'folders':
        return <FolderIcon />;
      case 'projects':
        return <QGLogo />;
      case 'files':
        return <FileIcon width="100%" />;
      case 'csvFiles':
        return (
          <Icon
            className="text-textAccentSecondary"
            component={() => <CSVFileIcon />}
          ></Icon>
        );
      case 'all':
        return <FilterIcon />;
    }
  }, [filter]);

  return (
    <Icon
      className={cx(`w-[${size}px]`, {
        'stroke-textSecondary text-textSecondary': !isTransparent,
        'stroke-transparent text-transparent': isTransparent,
      })}
      component={() => itemIcon}
    ></Icon>
  );
}

const Option = (props: any) => (
  <components.Option {...props}>
    <div className="flex item-center">
      <SelectIcon
        filter={props.data.value}
        isTransparent={props.data.value === 'projects'}
        size={16}
      />
      <span className="ml-2">{props.data.label}</span>
    </div>
  </components.Option>
);

export function DashboardFileListFilter() {
  const { filter, setFilter } = useContext(DashboardContext);

  const [selectedFilter, setSelectedFilter] = useState<DefaultOptionType>(
    filterOptions[0]
  );

  useEffect(() => {
    setSelectedFilter(
      filterOptions.find((i) => i.value === filter) || filterOptions[0]
    );
  }, [filter]);

  const onChange = useCallback(
    (option: SingleValue<DefaultOptionType>) => {
      setFilter(option?.value as DashboardFilter);
    },
    [setFilter]
  );

  return (
    <div className="flex items-center shrink-0">
      <SelectIcon
        filter={filter}
        isTransparent={filter === 'projects'}
        size={18}
      />
      <Select
        classNames={{
          ...SelectClasses,
          control: () =>
            cx(
              '!bg-bgLayer3 !border-0 hover:!border-none !shadow-none text-[14px]'
            ),
          valueContainer: () => '!pr-0',
          menu: () => '!bg-bgLayer0 text-[14px] !rounded-[3px] min-w-[120px]',
        }}
        components={{
          IndicatorSeparator: null,
          Option: Option,
        }}
        isSearchable={false}
        menuPortalTarget={document.body}
        name="fitlerSelect"
        options={filterOptions}
        styles={{
          menuPortal: (base) => ({ ...base, zIndex: 9999 }),
        }}
        value={selectedFilter}
        onChange={onChange}
      />
    </div>
  );
}

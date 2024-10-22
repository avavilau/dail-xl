import pytest

from dial_xl.overrides import Override, Overrides
from tests.common import create_sheet


@pytest.mark.asyncio
async def test_overrides():
    sheet = await create_sheet(
        "table A\n"
        "  [a] = NA\n"
        "  [b] = NA\n"
        "override\n"
        "row,[a],[b]\n"
        "1,2,3\n"
        "4,5,6\n"
    )
    table = sheet.get_table("A")
    overrides = table.overrides

    assert isinstance(overrides, Overrides)
    assert list(overrides.field_names) == ["a", "b"]
    assert overrides.row_position == 0
    assert overrides[0].row_number == "1"
    assert overrides[0]["a"] == "2"
    assert overrides[0]["b"] == "3"
    assert overrides[1].row_number == "4"
    assert overrides[1]["a"] == "5"
    assert overrides[1]["b"] == "6"


@pytest.mark.asyncio
async def test_add_override():
    sheet = await create_sheet("table A\n  [a] = NA\n")

    table = sheet.get_table("A")
    overrides = Overrides()
    overrides.append(Override({"a": "1"}))
    table.overrides = overrides

    assert list(overrides.field_names) == ["a"]
    assert len(overrides) == 1
    assert sheet.to_dsl() == "table A\n  [a] = NA\noverride\n[a]\n1\n"


@pytest.mark.asyncio
async def test_add_override_to_existing_line():
    sheet = await create_sheet(
        "table A\n" "  [a] = NA\n" "  [b] = NA\n" "override\n" "[a]\n" "1\n"
    )

    table = sheet.get_table("A")
    overrides = table.overrides
    overrides[0]["b"] = "2"

    assert list(overrides.field_names) == ["a", "b"]
    assert len(overrides) == 1
    assert (
        sheet.to_dsl()
        == "table A\n  [a] = NA\n  [b] = NA\noverride\n[a],[b]\n1,2\n"
    )


@pytest.mark.asyncio
async def test_add_override_line_to_existing():
    sheet = await create_sheet(
        "table A\n  [a] = NA\n  [b] = NA\noverride\n[a]\n1\n"
    )

    table = sheet.get_table("A")
    overrides = table.overrides
    overrides.append(Override({"b": "2"}))

    assert list(overrides.field_names) == ["a", "b"]
    assert len(overrides) == 2
    assert (
        sheet.to_dsl()
        == "table A\n  [a] = NA\n  [b] = NA\noverride\n[a],[b]\n1,\n,2\n"
    )


@pytest.mark.asyncio
async def test_update_override():
    sheet = await create_sheet("table A\n  [a] = NA\noverride\n[a]\n1\n")

    table = sheet.get_table("A")
    overrides = table.overrides
    overrides[0]["a"] = "2"

    assert list(overrides.field_names) == ["a"]
    assert len(overrides) == 1
    assert sheet.to_dsl() == "table A\n  [a] = NA\noverride\n[a]\n2\n"


@pytest.mark.asyncio
async def test_remove_override():
    sheet = await create_sheet(
        "table A\n  [a] = NA\n  [b] = NA\noverride\n[a],[b]\n1,2\n"
    )

    table = sheet.get_table("A")
    overrides = table.overrides
    del overrides[0]["b"]

    assert list(overrides.field_names) == ["a"]
    assert len(overrides) == 1
    assert (
        sheet.to_dsl() == "table A\n  [a] = NA\n  [b] = NA\noverride\n[a]\n1\n"
    )


@pytest.mark.asyncio
async def test_add_row_override():
    sheet = await create_sheet("table A\n  [a] = NA\n")
    table = sheet.get_table("A")
    overrides = Overrides()
    override = Override()
    override.row_number = "1"
    override["a"] = "2"
    overrides.append(override)
    table.overrides = overrides

    assert list(overrides.field_names) == ["a"]
    assert overrides.row_position == 0
    assert sheet.to_dsl() == "table A\n  [a] = NA\noverride\nrow,[a]\n1,2\n"


@pytest.mark.asyncio
async def test_add_row_to_override():
    sheet = await create_sheet("table A\n  [a] = NA\noverride\n[a]\n2\n")
    table = sheet.get_table("A")
    overrides = table.overrides
    overrides[0].row_number = "1"

    assert list(overrides.field_names) == ["a"]
    assert overrides.row_position == 1
    assert sheet.to_dsl() == "table A\n  [a] = NA\noverride\n[a],row\n2,1\n"


@pytest.mark.asyncio
async def test_remove_row_from_override():
    sheet = await create_sheet(
        "table A\n  [a] = NA\n  [b] = NA\noverride\n[a],row\n1,2\n"
    )

    table = sheet.get_table("A")
    overrides = table.overrides
    overrides[0].row_number = None

    assert list(overrides.field_names) == ["a"]
    assert len(overrides) == 1
    assert (
        sheet.to_dsl() == "table A\n  [a] = NA\n  [b] = NA\noverride\n[a]\n1\n"
    )


@pytest.mark.asyncio
async def test_update_override_line():
    sheet = await create_sheet("table A\n  [a] = NA\noverride\n[a]\n1\n")
    table = sheet.get_table("A")
    overrides = table.overrides
    overrides[0] = Override({"a": "2"})

    assert sheet.to_dsl() == "table A\n  [a] = NA\noverride\n[a]\n2\n"


@pytest.mark.asyncio
async def test_remove_override_line():
    sheet = await create_sheet("table A\n  [a] = NA\noverride\n[a]\n1\n2\n")
    table = sheet.get_table("A")
    overrides = table.overrides
    del overrides[1]

    assert sheet.to_dsl() == "table A\n  [a] = NA\noverride\n[a]\n1\n"

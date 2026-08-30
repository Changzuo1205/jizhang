with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'r') as f:
    content = f.read()

old_anim = """            AnimatedVisibility(
                visible = !isCategoryDrillDown || isSelected,
                enter = fadeIn() + expandHorizontally(),
                exit = fadeOut() + shrinkHorizontally()
            )"""

new_anim = """            AnimatedVisibility(
                visible = !isCategoryDrillDown || isSelected,
                enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
                exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
            )"""

content = content.replace(old_anim, new_anim)

old_anim_btn = """        AnimatedVisibility(
            visible = !isCategoryDrillDown,
            enter = fadeIn() + expandHorizontally(),
            exit = fadeOut() + shrinkHorizontally()
        )"""

new_anim_btn = """        AnimatedVisibility(
            visible = !isCategoryDrillDown,
            enter = fadeIn() + expandIn(expandFrom = Alignment.Center),
            exit = fadeOut() + shrinkOut(shrinkTowards = Alignment.Center)
        )"""

content = content.replace(old_anim_btn, new_anim_btn)

with open('app/src/main/java/com/example/ui/screens/EditorialExpenseAddEditScreen.kt', 'w') as f:
    f.write(content)

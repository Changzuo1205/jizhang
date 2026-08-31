sed -i 's/verticalArrangement = Arrangement.Bottom/verticalArrangement = Arrangement.Center/g' app/src/main/java/com/example/ui/components/BottomNavBar.kt
sed -i 's/.padding(bottom = 8.dp)//g' app/src/main/java/com/example/ui/components/BottomNavBar.kt
sed -i 's/.fillMaxSize()/.fillMaxHeight()/g' app/src/main/java/com/example/ui/components/BottomNavBar.kt

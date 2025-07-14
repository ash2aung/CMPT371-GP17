# CMPT371-GP17
CMPT371 Group Project - Race to the Cheese
## Contributors
Ash Aung
Kyaw Htet Aung
Khanh Doan
Brandon Chattha

## Objective
Start with a near-empty maze with a character in each corner. The goal is to collect 3 *objective item name* around the maze before anyone else.
## Mechanics
### General
Use 'WASD' to move your character in any cardinal direction. Walking through walls or spaces occupied by others is not allowed.
### *Objective Item*
Only 1 *objective item* is on the board at a time. As soon as it is collected by a player, a new one will randomly spawn. *objective item*s cannot spawn on walls.
### Enemies (NPCs) *Optional*
Enemies spawn on random empty tiles that are at least 3 steps away from every person. If an enemy moves onto the space occupied by a player or the player moves onto a space occupied by an enemy, the player is stunned until the enemy moves off the space. Each enemy moves every 2 seconds. The enemies are programmed to avoid backtracking as much as possible: if an enemy moves off of square A3 onto A4, it will only move back onto A3 if all other directions off of A4 are walls, i.e., there is no other space to move to. Here, backtracking is the only available move in which it will be taken.
### Players
Each player can move as fast as they can type. Moving onto a cell occupied by an enemy will result in a stun until the enemy moves, and moving onto the 3rd *objective item* will result in a win for that player.
### Maze
To begin, most of the map will be hidden as a space will only be shown if a player has been adjacent to it at some point during the current game. Unrevealed spaces will be displayed as *CHANGE ME*, revealed but empty spaces as *CHANGE ME*, walls as *CHANGE ME*, enemies as *CHANGE ME*, the players as *CHANGE ME*, and the *objective item* as *CHANGE ME*. Regardless of whether a player has discovered the space an enemy or the *objective item* is on, it will always be visible on the map. Additionally, the outer border will always be shown. Each player can see where every other player is and has been.

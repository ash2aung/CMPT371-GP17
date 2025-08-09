# CMPT371-GP17
CMPT371 Group Project - Race to the Cheese
## Contributors
Ash Aung<br>
Kyaw Htet Aung<br>
Khanh Doan<br>
Brandon Chattha<br>

## Objective
Start with a mostly invisible maze with a character in each corner. The goal is to collect 3 Cheese around the maze before anyone else.
## Mechanics
### General
Use 'WASD' to move your character in any cardinal direction. Walking through walls or spaces occupied by others is not allowed.
### Cheese
Only 1 *objective item* is on the board at a time. As soon as it is collected by a player, a new one will randomly spawn. *objective item*s cannot spawn on walls.
### Players
Each player can move as fast as they can type. Moving onto a cell occupied by an enemy will result in a stun until the enemy moves, and moving onto the 3rd *objective item* will result in a win for that player.
### Maze
To begin, most of the map will be hidden as a space will only be shown if a player has been adjacent to it at some point during the current game. Unrevealed spaces will be displayed as black tiles. Regardless of whether a player has discovered the space an enemy or the Cheese is on, it will always be visible on the map. Additionally, the outer border of walls will always be shown. Each player can see where every other player is and has been.
## How To Play
Clone the repository from the github link above and make sure you are on the main branch.

In the directory MazeGame, run the command “mvn javafx:run”.

(If you are a single person on a windows machine, you can run ./run4clients.bat which is a script that opens 4 terminals and run the start command in each, for convenient testing) 

The game requires 4 concurrent players. The Client attempts to connect to the server by pressing the “Start game” button. Once all 4 players have connected, the game will begin.


package com.example

import com.example.data.PlayerStatsRecord
import com.example.data.StatsRepository
import com.example.logic.GridCoord
import com.example.logic.LudoBoardMap
import com.example.logic.LudoEngine
import com.example.model.DiceState
import com.example.model.GameMode
import com.example.model.GameState
import com.example.model.Player
import com.example.model.PlayerColor
import com.example.model.Token
import com.example.model.TokenPosition
import com.example.model.TurnPhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    // Rule 1: A token can leave its base only when the player rolls a 6.
    @Test
    fun rule1_tokenCanOnlyLeaveBaseOnSix() {
        val token = Token(id = 0, color = PlayerColor.RED, position = TokenPosition.InBase)
        for (roll in 1..5) {
            assertFalse("Token should not be able to leave base on roll $roll", LudoEngine.canTokenMove(token, roll))
        }
        assertTrue("Token MUST be able to leave base on roll 6", LudoEngine.canTokenMove(token, 6))

        val player = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(token)
        )
        val state = GameState(
            players = listOf(player),
            currentTurnIndex = 0,
            diceState = DiceState(value = 6),
            phase = TurnPhase.SELECTING_TOKEN
        )
        val outcome = LudoEngine.executeMove(state, 0)
        assertEquals(TokenPosition.OnBoard(step = 0), outcome.updatedState.players[0].tokens[0].position)
        assertTrue("Rolling 6 to leave base should award bonus turn", outcome.hasBonusTurn)
    }

    // Rule 2: Each token follows the correct Ludo path and enters its own colored home lane.
    @Test
    fun rule2_tokensFollowCorrectLudoPathsAndEnterCorrectHomeLanes() {
        // Verify start indices on the 52-outer track
        assertEquals(0, PlayerColor.RED.startTrackIndex)
        assertEquals(13, PlayerColor.GREEN.startTrackIndex)
        assertEquals(26, PlayerColor.YELLOW.startTrackIndex)
        assertEquals(39, PlayerColor.BLUE.startTrackIndex)

        // Verify starting square coordinates directly in front of each base
        assertEquals(GridCoord(13, 6), LudoBoardMap.OUTER_TRACK[PlayerColor.RED.startTrackIndex])    // In front of Red Yard (Bottom Arm, moving UP)
        assertEquals(GridCoord(6, 1), LudoBoardMap.OUTER_TRACK[PlayerColor.GREEN.startTrackIndex])   // In front of Green Yard (Left Arm, moving RIGHT)
        assertEquals(GridCoord(1, 8), LudoBoardMap.OUTER_TRACK[PlayerColor.YELLOW.startTrackIndex])  // In front of Yellow Yard (Top Arm, moving DOWN)
        assertEquals(GridCoord(8, 13), LudoBoardMap.OUTER_TRACK[PlayerColor.BLUE.startTrackIndex])  // In front of Blue Yard (Right Arm, moving LEFT)

        // For all colors, verify track index wraps around 52 outer cells
        for (color in PlayerColor.values()) {
            for (step in 0..50) {
                val trackIndex = LudoBoardMap.getTrackIndex(color, step)
                assertNotNull(trackIndex)
                assertEquals((color.startTrackIndex + step) % 52, trackIndex)
            }
            // In home lane (steps 51..55), trackIndex is null because it's private
            for (step in 51..55) {
                assertNull(LudoBoardMap.getTrackIndex(color, step))
                assertTrue(LudoBoardMap.isSafeSquare(color, step))
            }
        }
    }

    // Rule 3: A token must roll the exact number needed to reach the final home square.
    @Test
    fun rule3_exactRollNeededToReachGoal() {
        val tokenAt55 = Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 55))
        assertTrue("Needs 1 to finish", LudoEngine.canTokenMove(tokenAt55, 1))
        assertFalse("Roll of 2 overshoots step 56", LudoEngine.canTokenMove(tokenAt55, 2))
        assertFalse("Roll of 6 overshoots step 56", LudoEngine.canTokenMove(tokenAt55, 6))

        val tokenAt53 = Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 53))
        assertTrue("Needs 3 to finish", LudoEngine.canTokenMove(tokenAt53, 3))
        assertFalse("Roll of 4 overshoots step 56", LudoEngine.canTokenMove(tokenAt53, 4))
    }

    // Rule 4: Landing on an opponent token captures it and sends it back to base, except on safe squares.
    @Test
    fun rule4_capturingOpponentSendsBackToBaseAndGrantsBonusTurn() {
        val redPlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 1)))
        )
        // Blue token on track index 3 (not a safe square: 3 is not in {0,8,13,21,26,34,39,47})
        // Blue start is 39: (39 + 16) % 52 = 55 % 52 = 3
        val bluePlayer = Player(
            color = PlayerColor.BLUE,
            name = "Blue",
            tokens = listOf(Token(id = 0, color = PlayerColor.BLUE, position = TokenPosition.OnBoard(step = 16)))
        )

        val state = GameState(
            players = listOf(redPlayer, bluePlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 2), // Red moves from 1 to 3
            phase = TurnPhase.SELECTING_TOKEN
        )

        val outcome = LudoEngine.executeMove(state, 0)
        assertNotNull("Capture must be recorded", outcome.capturedToken)
        assertEquals(PlayerColor.BLUE, outcome.capturedToken?.color)
        assertTrue("Capture grants extra turn", outcome.hasBonusTurn)
        assertEquals("Blue token must be returned to base", TokenPosition.InBase, outcome.updatedState.players[1].tokens[0].position)
    }

    // Rule 5: Safe squares must protect tokens correctly.
    @Test
    fun rule5_safeSquaresProtectFromCapture() {
        // Track index 8 is a star safe square
        val redPlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 6)))
        )
        // Blue on track index 8: (39 + 21) % 52 = 60 % 52 = 8
        val bluePlayer = Player(
            color = PlayerColor.BLUE,
            name = "Blue",
            tokens = listOf(Token(id = 0, color = PlayerColor.BLUE, position = TokenPosition.OnBoard(step = 21)))
        )

        val state = GameState(
            players = listOf(redPlayer, bluePlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 2), // Red moves from 6 to 8 (Safe Star)
            phase = TurnPhase.SELECTING_TOKEN
        )

        val outcome = LudoEngine.executeMove(state, 0)
        assertNull("Tokens on safe squares must NOT be captured", outcome.capturedToken)
        assertEquals("Blue token must remain on board safe square", TokenPosition.OnBoard(step = 21), outcome.updatedState.players[1].tokens[0].position)
    }

    // Rule 6: Rolling a 6 grants an extra turn. Apply three consecutive sixes rule correctly.
    @Test
    fun rule6_rollingSixGrantsExtraTurnAndThreeSixesForfeits() {
        val player1 = Player(color = PlayerColor.RED, name = "Red")
        val player2 = Player(color = PlayerColor.YELLOW, name = "Yellow")
        val state = GameState(
            players = listOf(player1, player2),
            currentTurnIndex = 0,
            diceState = DiceState(value = 1, consecutiveSixes = 0),
            phase = TurnPhase.WAITING_FOR_ROLL
        )

        // First 6
        val state1 = LudoEngine.rollDice(state, forcedRoll = 6)
        assertEquals(1, state1.diceState.consecutiveSixes)
        assertEquals(0, state1.currentTurnIndex)

        // Second 6
        val state2 = LudoEngine.rollDice(state1.copy(phase = TurnPhase.WAITING_FOR_ROLL), forcedRoll = 6)
        assertEquals(2, state2.diceState.consecutiveSixes)
        assertEquals(0, state2.currentTurnIndex)

        // Third 6 -> turn forfeited!
        val state3 = LudoEngine.rollDice(state2.copy(phase = TurnPhase.WAITING_FOR_ROLL), forcedRoll = 6)
        assertEquals(TurnPhase.TURN_TRANSITION, state3.phase)
        assertEquals(0, state3.diceState.consecutiveSixes)
        assertTrue(state3.statusMessage.contains("forfeited"))
    }

    // Rule 7: The next player's turn must be handled correctly when no legal move is available.
    @Test
    fun rule7_nextPlayerTurnHandledWhenNoLegalMovesAvailable() {
        val player1 = Player(color = PlayerColor.RED, name = "Red")
        val player2 = Player(color = PlayerColor.YELLOW, name = "Yellow")
        val state = GameState(
            players = listOf(player1, player2),
            currentTurnIndex = 0,
            diceState = DiceState(value = 1),
            phase = TurnPhase.WAITING_FOR_ROLL
        )

        val nextState = LudoEngine.rollDice(state, forcedRoll = 3)
        assertEquals(TurnPhase.TURN_TRANSITION, nextState.phase)
        assertTrue(nextState.movableTokenIds.isEmpty())
        assertTrue(nextState.statusMessage.contains("No legal moves"))
        assertEquals(0, nextState.diceState.consecutiveSixes)
    }

    // Rule 8: A player wins only after all four tokens reach the final home.
    @Test
    fun rule8_playerWinsOnlyAfterAllFourTokensReachHome() {
        val threeFinishedTokens = listOf(
            Token(id = 0, color = PlayerColor.RED, position = TokenPosition.Finished),
            Token(id = 1, color = PlayerColor.RED, position = TokenPosition.Finished),
            Token(id = 2, color = PlayerColor.RED, position = TokenPosition.Finished),
            Token(id = 3, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 55))
        )
        val playerRed = Player(color = PlayerColor.RED, name = "Red", tokens = threeFinishedTokens)
        val playerYellow = Player(color = PlayerColor.YELLOW, name = "Yellow")

        val state = GameState(
            players = listOf(playerRed, playerYellow),
            currentTurnIndex = 0,
            diceState = DiceState(value = 1),
            phase = TurnPhase.SELECTING_TOKEN
        )

        assertFalse("Player with 3 tokens finished is not winner yet", playerRed.isFinished)

        val outcome = LudoEngine.executeMove(state, 3)
        assertTrue("Player with all 4 tokens finished is finished", outcome.updatedState.players[0].isFinished)
        assertTrue("Game over should trigger after winning", outcome.updatedState.isGameOver)
        assertEquals(1, outcome.updatedState.winnerPodium.size)
        assertEquals("Red", outcome.updatedState.winnerPodium[0].name)
    }

    // Rule 9: Token advances exactly one square for each dice point for all 4 colors.
    @Test
    fun rule9_tokenAdvancesExactlyOneSquareForEachDicePointAllColors() {
        for (color in PlayerColor.values()) {
            var currentStep = 0
            var token = Token(id = 0, color = color, position = TokenPosition.OnBoard(step = 0))

            // Step-by-step forward advance across outer track and into home lane
            while (currentStep < 56) {
                val roll = minOf(6, 56 - currentStep)
                assertTrue("Token should be able to move by $roll", LudoEngine.canTokenMove(token, roll))

                val player = Player(color = color, name = color.title, tokens = listOf(token))
                val state = GameState(
                    players = listOf(player),
                    currentTurnIndex = 0,
                    diceState = DiceState(value = roll),
                    phase = TurnPhase.SELECTING_TOKEN
                )
                val outcome = LudoEngine.executeMove(state, 0)
                currentStep += roll

                val updatedToken = outcome.updatedState.players[0].tokens[0]
                if (currentStep == 56) {
                    assertEquals("Token should be finished at step 56", TokenPosition.Finished, updatedToken.position)
                } else {
                    assertEquals("Token step should advance by exactly $roll", TokenPosition.OnBoard(step = currentStep), updatedToken.position)
                }
                token = updatedToken
            }
        }
    }

    // Rule 10: In 2-player, 3-player, and 4-player modes, correct players and colors participate.
    @Test
    fun rule10_correctPlayersAndColorsInAllModes() {
        val oppositeRed = PlayerColor.YELLOW
        val oppositeGreen = PlayerColor.BLUE

        assertEquals(PlayerColor.YELLOW, oppositeRed)
        assertEquals(PlayerColor.BLUE, oppositeGreen)

        // All 8 safe squares are present
        assertEquals(8, LudoBoardMap.SAFE_TRACK_INDICES.size)
        assertTrue(LudoBoardMap.SAFE_TRACK_INDICES.contains(0))   // Red start
        assertTrue(LudoBoardMap.SAFE_TRACK_INDICES.contains(13))  // Green start
        assertTrue(LudoBoardMap.SAFE_TRACK_INDICES.contains(26))  // Yellow start
        assertTrue(LudoBoardMap.SAFE_TRACK_INDICES.contains(39))  // Blue start

        assertTrue(LudoBoardMap.SAFE_TRACK_INDICES.contains(8))   // Star 1
        assertTrue(LudoBoardMap.SAFE_TRACK_INDICES.contains(21))  // Star 2
        assertTrue(LudoBoardMap.SAFE_TRACK_INDICES.contains(34))  // Star 3
        assertTrue(LudoBoardMap.SAFE_TRACK_INDICES.contains(47))  // Star 4
    }

    // Feature 1: Capture Emoji Animation
    @Test
    fun feature1_captureEmojiAnimationTriggeredOnLegalCapture() {
        val redPlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 1)))
        )
        // Blue token on track index 3 (step 16 from Blue start 39 -> (39+16)%52 = 3)
        val bluePlayer = Player(
            color = PlayerColor.BLUE,
            name = "Blue",
            tokens = listOf(Token(id = 2, color = PlayerColor.BLUE, position = TokenPosition.OnBoard(step = 16)))
        )
        val state = GameState(
            players = listOf(redPlayer, bluePlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 2), // Red moves 1 -> 3
            phase = TurnPhase.SELECTING_TOKEN
        )

        val outcome = LudoEngine.executeMove(state, 0)
        assertNotNull("Capture event must be generated on legal capture", outcome.captureEvent)
        val event = outcome.captureEvent!!
        assertEquals(PlayerColor.RED, event.capturingPlayerColor)
        assertEquals(PlayerColor.BLUE, event.capturedPlayerColor)
        assertEquals(0, event.capturingTokenId)
        assertEquals(2, event.capturedTokenId)
        assertTrue("Capturing emoji must be celebratory (😎 or 🔥)", event.capturingEmoji in listOf("😎", "🔥"))
        assertTrue("Captured emoji must be crying/dizzy (😭 or 😵‍💫)", event.capturedEmoji in listOf("😭", "😵‍💫"))
        assertNotNull("Capture board coordinate must not be null", event.captureBoardCoord)
        assertNotNull("Captured base coordinate must not be null", event.capturedBaseCoord)

        // Verify that move without capture produces NO captureEvent
        val normalState = GameState(
            players = listOf(redPlayer, bluePlayer.copy(tokens = listOf(Token(id = 2, color = PlayerColor.BLUE, position = TokenPosition.InBase)))),
            currentTurnIndex = 0,
            diceState = DiceState(value = 2),
            phase = TurnPhase.SELECTING_TOKEN
        )
        val normalOutcome = LudoEngine.executeMove(normalState, 0)
        assertNull("Non-capture move must NOT produce captureEvent", normalOutcome.captureEvent)
    }

    // Feature 2: Automatic Token Movement when only 1 legal move exists
    @Test
    fun feature2_singleLegalMoveCalculationAndStatus() {
        val player = Player(
            color = PlayerColor.RED,
            name = "Red Player",
            isBot = false,
            tokens = listOf(
                Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 10)),
                Token(id = 1, color = PlayerColor.RED, position = TokenPosition.InBase),
                Token(id = 2, color = PlayerColor.RED, position = TokenPosition.InBase),
                Token(id = 3, color = PlayerColor.RED, position = TokenPosition.InBase)
            )
        )
        val state = GameState(
            players = listOf(player),
            currentTurnIndex = 0,
            phase = TurnPhase.WAITING_FOR_ROLL
        )

        // Roll 3: Only token 0 can move (tokens in base cannot move without 6)
        val rolledState = LudoEngine.rollDice(state, forcedRoll = 3)
        assertEquals(TurnPhase.SELECTING_TOKEN, rolledState.phase)
        assertEquals(1, rolledState.movableTokenIds.size)
        assertTrue(rolledState.movableTokenIds.contains(0))
        assertTrue("Status message should indicate auto-moving token", rolledState.statusMessage.contains("Auto-moving token"))

        // Roll with 2 tokens on board -> size 2 -> manual choice message
        val playerWithTwo = player.copy(
            tokens = listOf(
                Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 10)),
                Token(id = 1, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 20)),
                Token(id = 2, color = PlayerColor.RED, position = TokenPosition.InBase),
                Token(id = 3, color = PlayerColor.RED, position = TokenPosition.InBase)
            )
        )
        val stateTwo = GameState(
            players = listOf(playerWithTwo),
            currentTurnIndex = 0,
            phase = TurnPhase.WAITING_FOR_ROLL
        )
        val rolledStateTwo = LudoEngine.rollDice(stateTwo, forcedRoll = 3)
        assertEquals(2, rolledStateTwo.movableTokenIds.size)
        assertTrue("Status message should prompt player to select a token", rolledStateTwo.statusMessage.contains("Select a token to move"))

        // Blocked roll (step 54 + 4 = 58 > 56) -> no legal move
        val blockedPlayer = player.copy(
            tokens = listOf(
                Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 54)),
                Token(id = 1, color = PlayerColor.RED, position = TokenPosition.InBase),
                Token(id = 2, color = PlayerColor.RED, position = TokenPosition.InBase),
                Token(id = 3, color = PlayerColor.RED, position = TokenPosition.InBase)
            )
        )
        val stateBlocked = GameState(
            players = listOf(blockedPlayer),
            currentTurnIndex = 0,
            phase = TurnPhase.WAITING_FOR_ROLL
        )
        val rolledBlocked = LudoEngine.rollDice(stateBlocked, forcedRoll = 4)
        assertEquals(TurnPhase.TURN_TRANSITION, rolledBlocked.phase)
        assertEquals(0, rolledBlocked.movableTokenIds.size)
        assertTrue("Status message should indicate no legal moves", rolledBlocked.statusMessage.contains("No legal moves"))
    }

    // Feature 1 edge case: No capture event on safe squares
    @Test
    fun feature1_noCaptureEventOnSafeSquare() {
        val redPlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 6)))
        )
        // Blue on track index 8 (Safe star) -> step 21
        val bluePlayer = Player(
            color = PlayerColor.BLUE,
            name = "Blue",
            tokens = listOf(Token(id = 1, color = PlayerColor.BLUE, position = TokenPosition.OnBoard(step = 21)))
        )
        val state = GameState(
            players = listOf(redPlayer, bluePlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 2), // Red moves 6 -> 8
            phase = TurnPhase.SELECTING_TOKEN
        )

        val outcome = LudoEngine.executeMove(state, 0)
        assertNull("Tokens on safe squares must not be captured", outcome.capturedToken)
        assertNull("Capture event must NOT be triggered on safe square", outcome.captureEvent)
        assertEquals("Blue token must still be on board", TokenPosition.OnBoard(step = 21), outcome.updatedState.players[1].tokens[0].position)
    }

    // Feature 3: Winner celebration podium and game over state
    @Test
    fun feature3_winnerPodiumCelebrationState() {
        val threeTokensFinished = listOf(
            Token(id = 0, color = PlayerColor.GREEN, position = TokenPosition.Finished),
            Token(id = 1, color = PlayerColor.GREEN, position = TokenPosition.Finished),
            Token(id = 2, color = PlayerColor.GREEN, position = TokenPosition.Finished),
            Token(id = 3, color = PlayerColor.GREEN, position = TokenPosition.OnBoard(step = 54))
        )
        val greenPlayer = Player(color = PlayerColor.GREEN, name = "Green Champion", tokens = threeTokensFinished)
        val yellowPlayer = Player(color = PlayerColor.YELLOW, name = "Yellow Runner Up")

        val state = GameState(
            players = listOf(greenPlayer, yellowPlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 2), // 54 + 2 = 56 (Goal)
            phase = TurnPhase.SELECTING_TOKEN
        )

        val outcome = LudoEngine.executeMove(state, 3)
        assertTrue("Game over should trigger when player wins", outcome.updatedState.isGameOver)
        assertEquals(1, outcome.updatedState.winnerPodium.size)
        val winner = outcome.updatedState.winnerPodium[0]
        assertEquals("Green Champion", winner.name)
        assertEquals(1, winner.rank)
        assertEquals(4, winner.finishedTokensCount)
        assertTrue(outcome.updatedState.statusMessage.contains("wins the crown"))
    }

    // Statistics Rule 1 & 2: Calculate win percentage as (wins / completed) * 100, display "—" if 0 games
    @Test
    fun testStats_winningPercentageCalculation() {
        // Zero completed games displays "—"
        assertEquals("—", StatsRepository.calculateWinPercentage(wins = 0, completedGames = 0))
        val newPlayerRecord = PlayerStatsRecord(
            id = "human:jitu",
            name = "Jitu",
            color = PlayerColor.RED,
            isBot = false,
            wins = 0,
            completedGames = 0
        )
        assertEquals("New player with 0 games must show '—'", "—", newPlayerRecord.winPercentageFormatted)

        // 0 wins in 2 completed games displays 0%
        assertEquals("0%", StatsRepository.calculateWinPercentage(wins = 0, completedGames = 2))

        // 1 win in 2 completed games displays 50%
        assertEquals("50%", StatsRepository.calculateWinPercentage(wins = 1, completedGames = 2))

        // 2 wins in 2 completed games displays 100%
        assertEquals("100%", StatsRepository.calculateWinPercentage(wins = 2, completedGames = 2))

        // 1 win in 3 completed games displays 33.3%
        assertEquals("33.3%", StatsRepository.calculateWinPercentage(wins = 1, completedGames = 3))
    }

    // Statistics Rule 6, 8, 9: Stable player identifier for Pass & Play and Vs Computer
    @Test
    fun testStats_stablePlayerIdGeneration() {
        val human1 = Player(color = PlayerColor.RED, name = "Alice", isBot = false)
        val human2 = Player(color = PlayerColor.YELLOW, name = "Bob", isBot = false)
        val humanWithSpaces = Player(color = PlayerColor.GREEN, name = "  Charlie  ", isBot = false)

        val botRed = Player(color = PlayerColor.RED, name = "Bot Red", isBot = true)
        val botYellow = Player(color = PlayerColor.YELLOW, name = "Bot Yellow", isBot = true)

        // Human players tracked by name
        assertEquals("human:alice", StatsRepository.buildPlayerId(human1))
        assertEquals("human:bob", StatsRepository.buildPlayerId(human2))
        assertEquals("human:charlie", StatsRepository.buildPlayerId(humanWithSpaces))

        // Computer bots tracked separately by color
        assertEquals("bot:red", StatsRepository.buildPlayerId(botRed))
        assertEquals("bot:yellow", StatsRepository.buildPlayerId(botYellow))

        // Different named players in Pass & Play produce distinct IDs
        assertFalse(StatsRepository.buildPlayerId(human1) == StatsRepository.buildPlayerId(human2))

        // Human vs Bot with same color produces distinct IDs
        assertFalse(StatsRepository.buildPlayerId(human1) == StatsRepository.buildPlayerId(botRed))
    }

    // Win Probability Requirements: Equal at game start, updates dynamically as tokens move/capture/finish
    @Test
    fun test_liveWinChanceInitialEqualityAndDynamicUpdates() {
        // 2 players at game start -> exactly 0% / 0%
        val p1 = Player(color = PlayerColor.RED, name = "Red")
        val p2 = Player(color = PlayerColor.YELLOW, name = "Yellow")
        val initial2 = LudoEngine.calculateLiveWinChances(listOf(p1, p2))
        assertEquals(0, initial2[0].liveWinChancePercent)
        assertEquals(0, initial2[1].liveWinChancePercent)
        assertEquals("0%", initial2[0].formattedWinChance)
        assertEquals("0%", initial2[1].formattedWinChance)

        // 3 players at game start -> exactly 0% each
        val p3 = Player(color = PlayerColor.GREEN, name = "Green")
        val initial3 = LudoEngine.calculateLiveWinChances(listOf(p1, p2, p3))
        for (p in initial3) {
            assertEquals(0, p.liveWinChancePercent)
            assertEquals("0%", p.formattedWinChance)
        }

        // 4 players at game start -> exactly 0% each
        val p4 = Player(color = PlayerColor.BLUE, name = "Blue")
        val initial4 = LudoEngine.calculateLiveWinChances(listOf(p1, p2, p3, p4))
        for (p in initial4) {
            assertEquals(0, p.liveWinChancePercent)
            assertEquals("0%", p.formattedWinChance)
        }

        // As Red moves out and advances, Red's win chance rises
        val advancedP1 = p1.copy(tokens = listOf(
            Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 30)),
            Token(id = 1, color = PlayerColor.RED, position = TokenPosition.InBase),
            Token(id = 2, color = PlayerColor.RED, position = TokenPosition.InBase),
            Token(id = 3, color = PlayerColor.RED, position = TokenPosition.InBase)
        ))
        val updatedChances = LudoEngine.calculateLiveWinChances(listOf(advancedP1, p2))
        assertTrue("Advanced player win chance must rise above 50%", updatedChances[0].liveWinChancePercent > 50)
        assertTrue("Trailing player win chance must drop below 50%", updatedChances[1].liveWinChancePercent < 50)
        assertEquals(100, updatedChances.sumOf { it.liveWinChancePercent })

        // When a player finishes all tokens, they have 100%
        val winnerP1 = p1.copy(tokens = List(4) { Token(id = it, color = PlayerColor.RED, position = TokenPosition.Finished) })
        val finishedChances = LudoEngine.calculateLiveWinChances(listOf(winnerP1, p2))
        assertEquals(100, finishedChances[0].liveWinChancePercent)
        assertEquals(0, finishedChances[1].liveWinChancePercent)
        assertEquals("100%", finishedChances[0].formattedWinChance)
        assertEquals("0%", finishedChances[1].formattedWinChance)
    }

    // Combat Kills & Losses Counter Tracking
    @Test
    fun test_combatCapturesAndLossesTracking() {
        val redPlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 1))),
            captures = 0,
            losses = 0
        )
        // Blue token on track index 3 (step 16)
        val bluePlayer = Player(
            color = PlayerColor.BLUE,
            name = "Blue",
            tokens = listOf(Token(id = 1, color = PlayerColor.BLUE, position = TokenPosition.OnBoard(step = 16))),
            captures = 0,
            losses = 0
        )
        val state = GameState(
            players = listOf(redPlayer, bluePlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 2),
            phase = TurnPhase.SELECTING_TOKEN
        )

        val outcome = LudoEngine.executeMove(state, 0)
        val updatedRed = outcome.updatedState.players.find { it.color == PlayerColor.RED }!!
        val updatedBlue = outcome.updatedState.players.find { it.color == PlayerColor.BLUE }!!

        assertEquals(1, updatedRed.captures)
        assertEquals(0, updatedRed.losses)
        assertEquals(0, updatedBlue.captures)
        assertEquals(1, updatedBlue.losses)
    }

    // Audio Event Triggers & Sound Toggle Validation
    @Test
    fun test_audioEventTriggersAndSoundToggle() {
        val defaultState = GameState()
        assertTrue("Sound should be enabled by default", defaultState.soundEnabled)

        val mutedState = defaultState.copy(soundEnabled = false)
        assertFalse("Sound toggle should disable sound", mutedState.soundEnabled)

        // 1. Capture Event Trigger: ensures capturedToken is non-null for capture sound
        val redPlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 1)))
        )
        val bluePlayer = Player(
            color = PlayerColor.BLUE,
            name = "Blue",
            tokens = listOf(Token(id = 1, color = PlayerColor.BLUE, position = TokenPosition.OnBoard(step = 16)))
        )
        val captureState = GameState(
            players = listOf(redPlayer, bluePlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 2),
            phase = TurnPhase.SELECTING_TOKEN
        )
        val captureOutcome = LudoEngine.executeMove(captureState, 0)
        assertNotNull("Capture outcome must have non-null capturedToken to trigger capture sound", captureOutcome.capturedToken)

        // 2. Safe Star Move: ensures NO capture sound triggered on safe square
        val safeBluePlayer = Player(
            color = PlayerColor.BLUE,
            name = "Blue",
            tokens = listOf(Token(id = 1, color = PlayerColor.BLUE, position = TokenPosition.OnBoard(step = 21)))
        )
        val safeState = GameState(
            players = listOf(redPlayer.copy(tokens = listOf(Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 6))))),
            currentTurnIndex = 0,
            diceState = DiceState(value = 2),
            phase = TurnPhase.SELECTING_TOKEN
        )
        val safeOutcome = LudoEngine.executeMove(safeState, 0)
        assertNull("Safe star move must NOT trigger capture sound", safeOutcome.capturedToken)

        // 3. Home Arrival Trigger: ensures bonusReason contains 'Home' to trigger home sound
        val nearHomePlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(
                Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 55)),
                Token(id = 1, color = PlayerColor.RED, position = TokenPosition.InBase),
                Token(id = 2, color = PlayerColor.RED, position = TokenPosition.InBase),
                Token(id = 3, color = PlayerColor.RED, position = TokenPosition.InBase)
            )
        )
        val homeState = GameState(
            players = listOf(nearHomePlayer, bluePlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 1),
            phase = TurnPhase.SELECTING_TOKEN
        )
        val homeOutcome = LudoEngine.executeMove(homeState, 0)
        assertTrue("Reaching home must grant bonus turn with 'Home' in reason", homeOutcome.bonusReason?.contains("Home") == true)

        // 4. Winner / Game Over Trigger: ensures victory fanfare is triggered
        val almostWonPlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(
                Token(id = 0, color = PlayerColor.RED, position = TokenPosition.Finished),
                Token(id = 1, color = PlayerColor.RED, position = TokenPosition.Finished),
                Token(id = 2, color = PlayerColor.RED, position = TokenPosition.Finished),
                Token(id = 3, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 55))
            )
        )
        val victoryState = GameState(
            players = listOf(almostWonPlayer, bluePlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 1),
            phase = TurnPhase.SELECTING_TOKEN
        )
        val victoryOutcome = LudoEngine.executeMove(victoryState, 3)
        assertTrue("Completing all 4 tokens must set isGameOver for victory fanfare", victoryOutcome.updatedState.isGameOver)
    }

    // Issue 1 Test: Rolling 6 brings token out, rolling 6 again allows selecting token that just left base or base token
    @Test
    fun test_rollingSixTwiceTokenSelectionAndMovement() {
        val redPlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = List(4) { Token(id = it, color = PlayerColor.RED, position = TokenPosition.InBase) }
        )
        val bluePlayer = Player(
            color = PlayerColor.BLUE,
            name = "Blue",
            tokens = List(4) { Token(id = it, color = PlayerColor.BLUE, position = TokenPosition.InBase) }
        )
        val initialState = GameState(
            players = listOf(redPlayer, bluePlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 1, consecutiveSixes = 0),
            phase = TurnPhase.WAITING_FOR_ROLL
        )

        // 1. Roll 6 on first turn
        val stateAfterRoll1 = LudoEngine.rollDice(initialState, forcedRoll = 6)
        assertEquals(TurnPhase.SELECTING_TOKEN, stateAfterRoll1.phase)
        assertEquals(setOf(0, 1, 2, 3), stateAfterRoll1.movableTokenIds)

        // Player selects token 0 to move out of base
        val outcome1 = LudoEngine.executeMove(stateAfterRoll1, 0)
        assertTrue("Rolling 6 must award bonus turn", outcome1.hasBonusTurn)
        val redTokensAfter1 = outcome1.updatedState.players[0].tokens
        assertEquals(TokenPosition.OnBoard(step = 0), redTokensAfter1[0].position)
        assertEquals(0, outcome1.updatedState.currentTurnIndex)
        assertEquals(TurnPhase.WAITING_FOR_ROLL, outcome1.updatedState.phase)

        // 2. Roll 6 again on bonus turn
        val stateAfterRoll2 = LudoEngine.rollDice(outcome1.updatedState, forcedRoll = 6)
        assertEquals(TurnPhase.SELECTING_TOKEN, stateAfterRoll2.phase)
        assertEquals(2, stateAfterRoll2.diceState.consecutiveSixes)

        // Verify all 4 tokens can legally move: Token 0 (at step 0 -> step 6), and Tokens 1, 2, 3 (InBase -> step 0)
        assertEquals(setOf(0, 1, 2, 3), stateAfterRoll2.movableTokenIds)
        val curRed = stateAfterRoll2.players[0]
        assertTrue("Token that just left base must be legal to move on roll 6", LudoEngine.canTokenMove(curRed.tokens[0], 6))
        assertTrue("Base token must be legal to move out on roll 6", LudoEngine.canTokenMove(curRed.tokens[1], 6))

        // 3. Move token 0 (which just left base) forward by 6
        val outcome2 = LudoEngine.executeMove(stateAfterRoll2, 0)
        val redTokensAfter2 = outcome2.updatedState.players[0].tokens
        assertEquals(TokenPosition.OnBoard(step = 6), redTokensAfter2[0].position)
        assertTrue("Second roll of 6 also awards bonus turn", outcome2.hasBonusTurn)
    }

    // Issue 1 Test: Single legal move vs multiple choices
    @Test
    fun test_singleLegalMoveResolution() {
        val redPlayer = Player(
            color = PlayerColor.RED,
            name = "Red",
            tokens = listOf(
                Token(id = 0, color = PlayerColor.RED, position = TokenPosition.OnBoard(step = 10)),
                Token(id = 1, color = PlayerColor.RED, position = TokenPosition.InBase),
                Token(id = 2, color = PlayerColor.RED, position = TokenPosition.InBase),
                Token(id = 3, color = PlayerColor.RED, position = TokenPosition.InBase)
            )
        )
        val state = GameState(
            players = listOf(redPlayer),
            currentTurnIndex = 0,
            diceState = DiceState(value = 1),
            phase = TurnPhase.WAITING_FOR_ROLL
        )

        // Rolling 3: only Token 0 can move (tokens in base cannot move on non-6)
        val rolledState = LudoEngine.rollDice(state, forcedRoll = 3)
        assertEquals(setOf(0), rolledState.movableTokenIds)
        assertEquals(1, rolledState.movableTokenIds.size)
    }
}



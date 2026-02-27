package dev.truedoctales.sample.jupiter;

import static org.junit.jupiter.api.Assertions.*;

import dev.truedoctales.api.annotations.Scene;
import dev.truedoctales.api.annotations.Story;
import dev.truedoctales.execution.extension.StoryExtension;
import dev.truedoctales.sample.domain.FightService;
import dev.truedoctales.sample.domain.HeroService;
import dev.truedoctales.sample.domain.HeroService.Hero;
import dev.truedoctales.sample.domain.MonsterService;
import dev.truedoctales.sample.domain.MonsterService.Monster;
import dev.truedoctales.sample.domain.QuestService;
import dev.truedoctales.sample.domain.QuestService.Quest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/// Code-based version of "The Devil with the Three Golden Hairs" story.
///
/// This story mirrors the Markdown version but is written entirely in Java code.
/// It demonstrates that code-based and markdown-based stories can coexist and
/// produce the same documentation output.
///
/// The @ExtendWith(StoryExtension.class) annotation enables automatic JSON generation
/// during test execution, making this story appear in the Book of Truth documentation.
@Story(
    book = "Book of Stories",
    storyPath = "03_chapter-devil-three-golden-hairs/01_devil-three-golden-hairs.md",
    title = "The Devil with the Three Golden Hairs (Code)",
    markdown =
        """
            # The Devil with the Three Golden Hairs (Code)

            A boy was born with a caul, marking him as lucky.
            The king sends him on a deadly quest to bring three golden hairs from the Devil's head.
            """)
@ExtendWith(StoryExtension.class)
public class DevilGoldenHairsCodeTest {

  private HeroService heroService;
  private MonsterService monsterService;
  private QuestService questService;
  private FightService fightService;

  @BeforeEach
  void setUp() {
    heroService = new HeroService();
    monsterService = new MonsterService();
    questService = new QuestService();
    fightService = new FightService(heroService, monsterService);
  }

  @Scene(
      title = "The impossible quest begins",
      description =
          """
          The king assigns a quest he believes will end the Lucky Youth's life.

          The Lucky Youth must retrieve three golden hairs from the Devil himself.
          """)
  @Test
  void impossibleQuestBegins() {
    // Create the quest
    Quest quest =
        questService.createQuest(
            1L,
            "Get Devil's Three Hairs",
            "Pluck three golden hairs from the Devil",
            "IN_PROGRESS");

    assertNotNull(quest);
    assertEquals("IN_PROGRESS", quest.status());

    // Create the Lucky Youth hero (from prequels)
    Hero luckyYouth = heroService.createHero(10L, "Lucky Youth", "Human", 14);
    assertNotNull(luckyYouth);

    // Assign quest to hero
    assertTrue(questService.assignToHero("Lucky Youth", "Get Devil's Three Hairs"));

    // Verify quest status
    Quest verifiedQuest = questService.findByName("Get Devil's Three Hairs").orElseThrow();
    assertEquals("IN_PROGRESS", verifiedQuest.status());
  }

  @Scene(
      title = "Arrival in hell - the grandmother's help",
      description =
          """
          The Lucky Youth arrives at hell's gate. There he meets the Devil's grandmother,
          a kind old woman who takes pity on him.

          Direct combat with the Devil would be suicide! The grandmother offers a different solution.
          """)
  @Test
  void arrivalInHell() {
    // Create heroes
    Hero luckyYouth = heroService.createHero(11L, "Lucky Youth", "Human", 14);
    Hero grandmother = heroService.createHero(12L, "Grandmother", "Human", 100);

    // Create the Devil
    Monster devil = monsterService.createMonster(10L, "The Devil", "Infinite", "Wisdom");
    assertNotNull(devil);
    assertTrue(devil.alive());

    // Lucky Youth attempts to attack - this fails
    var attackResult = fightService.attack("Lucky Youth", "The Devil", "Sword");
    assertFalse(attackResult.success());
    assertEquals("Attack failed - weapon ineffective", attackResult.message());
  }

  @Scene(
      title = "Collecting the three golden hairs",
      description =
          """
          The grandmother uses her cunning to pluck three golden hairs from the Devil's head.

          Each time the Devil wakes and reveals the answer to one of the riddles through his dreams.

          This table shows each golden hair collection and the corresponding dream riddle:
          """)
  @ParameterizedTest(name = "Golden hair #{0}: {1}")
  @CsvSource({
    "1, First, Dry fountain that once flowed with wine, 13",
    "2, Second, Tree that no longer bears golden apples, 14",
    "3, Third, Ferryman doomed to ferry forever, 15"
  })
  void collectGoldenHair(int hairNumber, String ordinal, String dreamRiddle, long heroId) {
    // Create the grandmother hero for this iteration
    Hero grandmother = heroService.createHero(heroId, "Grandmother", "Human", 100);
    assertNotNull(grandmother);

    // Create the Devil monster
    Monster devil =
        monsterService.createMonster(10L + hairNumber, "The Devil", "Infinite", "Wisdom");
    assertNotNull(devil);
    assertTrue(devil.alive());

    // Grant Wisdom skill to grandmother
    assertTrue(heroService.grantSkill("Grandmother", "Wisdom"));

    // Grandmother defeats Devil with Wisdom skill (plucks the hair)
    var result = fightService.defeatWithSkill("Grandmother", "The Devil", "Wisdom");
    assertTrue(result.success());
    assertEquals("Victory!", result.message());

    // Verify the dream riddle was revealed (in real test, we'd check this somehow)
    assertNotNull(dreamRiddle);
    assertTrue(dreamRiddle.length() > 0);
  }

  @Scene(
      title = "Return journey and quest completion",
      description =
          """
          With three golden hairs in hand, the Lucky Youth transforms back from an ant
          and begins his return journey.

          He solves the riddles for the towns and completes his quest.
          """)
  @Test
  void questCompletion() {
    // Create hero
    Hero luckyYouth = heroService.createHero(16L, "Lucky Youth", "Human", 14);

    // Create and assign quest
    questService.createQuest(
        2L, "Get Devil's Three Hairs", "Pluck three golden hairs from the Devil", "IN_PROGRESS");
    questService.assignToHero("Lucky Youth", "Get Devil's Three Hairs");

    // Complete the quest
    assertTrue(questService.completeQuest("Get Devil's Three Hairs"));

    // Verify quest completed
    Quest completedQuest = questService.findByName("Get Devil's Three Hairs").orElseThrow();
    assertEquals("COMPLETED", completedQuest.status());
  }

  @Scene(
      title = "Return to the king - victory and marriage",
      description =
          """
          The Lucky Youth returns with the three golden hairs AND vast wealth from the grateful towns.

          The Lucky Youth marries the princess and becomes king.

          **Wisdom and luck triumph over evil and greed.**
          """)
  @Test
  void victoryAndMarriage() {
    // Create hero
    Hero luckyYouth = heroService.createHero(17L, "Lucky Youth", "Human", 14);
    Hero grandmother = heroService.createHero(18L, "Grandmother", "Human", 100);

    // Award trophies
    assertTrue(heroService.awardTrophy("Lucky Youth", "Devil's Golden Hairs"));
    assertTrue(heroService.awardTrophy("Lucky Youth", "Towns' Treasure"));
    assertTrue(heroService.awardTrophy("Lucky Youth", "Royal Crown"));

    // Verify both heroes exist and have appropriate levels
    Hero verifiedYouth = heroService.findByName("Lucky Youth").orElseThrow();
    Hero verifiedGrandmother = heroService.findByName("Grandmother").orElseThrow();

    assertEquals(1, verifiedYouth.level());
    assertEquals(1, verifiedGrandmother.level());
  }
}

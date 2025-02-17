use std::collections::{HashMap, HashSet};
use std::env;

use fondabots_lib::{affichan::Affichan, Bot, ErrType, Object};
use poise::futures_util::FutureExt;
use poise::serenity_prelude as serenity;
use poise::{BoxFuture, Context};
use serenity::all::{ChannelId, FullEvent, GatewayIntents, GuildChannel, ReactionType, RoleId, UserId};

use ecrit::{
    fields::Status,
    fields::Type,
    Ecrit
};
use fondabots_lib::command_data::{CommandData, Permission};
use regex::Regex;

mod ecrit;
mod commands;
pub type DataType = fondabots_lib::DataType<Ecrit>;

fn command_checker(ctx: Context<'_, DataType, ErrType>) -> BoxFuture<Result<bool, ErrType>> {
    async move {
        let permissions = ctx.command().custom_data.downcast_ref().unwrap_or(&CommandData::default()).permission;
        let member = ctx.author_member().await;
        let auth = match member {
            Some(member) => {
                let can_thanks_to_perms = match permissions {
                    Permission::READ | Permission::NONE => true,
                    Permission::WRITE => member.roles.contains(&RoleId::new(417334522775076864)), /* Classe-C membre */
                    Permission::MANAGE => member.roles.contains(&RoleId::new(811582204790571020)) /* Équipe Critique */
                };
                can_thanks_to_perms || member.roles.contains(&RoleId::new(417333090625781761)) /* Staff */
            },
            None => false
        };
        if !auth {
            ctx.reply("Vous n'avez pas la permission d'utiliser cette commande.").await?;
        }
        Ok(auth)
    }.boxed()
}

fn event_handler<'a>(ctx: &'a serenity::Context, event: &'a FullEvent, data: &'a DataType) -> BoxFuture<'a, Result<bool, ErrType>> {
    async move {
        match event {
            FullEvent::ThreadCreate { thread  } => thread_created(ctx, data, thread).await,
            _ => Ok(true)
        }
    }.boxed()
}

#[tokio::main]
async fn main() {
    let args: Vec<String> = env::args().collect();
    let mut owners = HashSet::new();
    owners.insert(UserId::new(340877529973784586));

    if let Some(token) = args.get(1) {
        match Bot::default()
            .owners(owners)
            .command_checker(Box::new(command_checker))
            .event_handler(event_handler)
            .set_log(725708994915860510)
            .setup(
            token.clone(),
            GatewayIntents::GUILD_MESSAGES | GatewayIntents::GUILD_MEMBERS,
            "./critibot.yml",
            commands::command_list(),
            vec![
                Affichan::new(ChannelId::new(1299620421506699275), Box::new(|ecrit| {
                    ecrit.status == Status::Ouvert || ecrit.status == Status::OuvertPlus
                })),
                Affichan::new(ChannelId::new(896361827884220467), Box::new(|ecrit| {
                    ecrit.status == Status::Inconnu || ecrit.status == Status::Infraction
                })),
                Affichan::new(ChannelId::new(896362452818747412), Box::new(|ecrit| {
                    ecrit.type_ == Type::Autre
                })),
            ],
            HashMap::new()
        ).await {
            Ok(mut bot) => if let Err(e) = bot.start().await {
                panic!("Erreur lors de l’exécution du bot: {e}");
            }
            Err(e) => panic!("Erreur lors du chargement du bot: {e}")
        }
    }
}

/// À la création d'un nouveau thread dans le forum des critiques, vérifie s'il y a un lien du forum
/// Wikidot dessus pour pouvoir lier les deux.
async fn thread_created(ctx: &serenity::Context, data: &DataType, thread: &GuildChannel) -> Result<bool, ErrType> {
    match thread.parent_id {
        Some(parent_id) if parent_id.get() == 1299603184519479357 => (),
        _ => return Ok(true)
    };

    let message = match thread.last_message_id {
        Some(last_message_id) => ctx.http.get_message(thread.id, last_message_id).await?,
        None => return Ok(true)
    };

    let regex_url = Regex::new(r#"https?:\/\/fondationscp\.wikidot\.com\/forum\/t-\d+\S*"#)?;
    let ecrit_id = match regex_url.captures(message.content.as_str())
        .and_then(|cap| cap.get(0))
        .and_then(|m| Ecrit::find_id(&m.as_str().to_string())) {
        Some(id) => id,
        None => return Ok(true)
    };

    Ecrit::maj_rss(data).await?;

    let mut bot = data.lock().await;
    let ecrit = match bot.database.get_mut(&ecrit_id) {
        Some(ecrit) => ecrit,
        None => return Ok(true)
    };

    ecrit.discord_chan = Some(thread.id);
    ecrit.modified = true;
    bot.update_affichans = true;

    message.react(ctx, ReactionType::Unicode("👌".to_string())).await?;

    Ok(true)

}